/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.cloud.rpc.enhancement.instrument.scg;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * EnhancedGatewayGlobalFilter.
 *
 * @author sean yu
 */
public class EnhancedGatewayGlobalFilter implements GlobalFilter, Ordered {

	private final EnhancedPluginRunner pluginRunner;

	public EnhancedGatewayGlobalFilter(EnhancedPluginRunner pluginRunner) {
		this.pluginRunner = pluginRunner;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange originExchange, GatewayFilterChain chain) {
		Response<ServiceInstance> serviceInstanceResponse = originExchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR);
		String serviceId = Optional.ofNullable(serviceInstanceResponse).map(Response::getServer).
				map(ServiceInstance::getServiceId).orElse(null);

		EnhancedPluginContext enhancedPluginContext = new EnhancedPluginContext();

		EnhancedRequestContext enhancedRequestContext = EnhancedRequestContext.builder()
				.httpHeaders(originExchange.getRequest().getHeaders())
				.httpMethod(originExchange.getRequest().getMethod())
				.url(originExchange.getRequest().getURI())
				.serviceUrl(getServiceUri(originExchange, serviceId))
				.build();
		enhancedPluginContext.setRequest(enhancedRequestContext);
		enhancedPluginContext.setOriginRequest(originExchange);

		// Run pre enhanced plugins.
		try {
			pluginRunner.run(EnhancedPluginType.Client.PRE, enhancedPluginContext);
		}
		catch (CallAbortedException e) {
			if (e.getFallbackInfo() == null) {
				throw e;
			}
			// circuit breaker fallback, not need to run post/exception enhanced plugins.
			ServerHttpResponse response = originExchange.getResponse();
			HttpStatus httpStatus = HttpStatus.resolve(e.getFallbackInfo().getCode());
			response.setStatusCode(httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR);
			if (CollectionUtils.isNotEmpty(e.getFallbackInfo().getHeaders())) {
				e.getFallbackInfo().getHeaders().forEach(response.getHeaders()::set);
			}
			String body = Optional.of(e.getFallbackInfo().getBody()).orElse("");
			DataBuffer dataBuffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
			return response.writeWith(Mono.just(dataBuffer));
		}
		// Exchange may be changed in plugin
		ServerWebExchange exchange = (ServerWebExchange) enhancedPluginContext.getOriginRequest();
		long startTime = System.currentTimeMillis();
		return chain.filter(exchange)
				.doOnSubscribe(v -> {
					Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
					URI uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
					enhancedPluginContext.getRequest().setUrl(uri);
					if (uri != null) {
						if (route != null && route.getUri().getScheme().contains("lb") && StringUtils.isNotEmpty(serviceId)) {
							DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
							serviceInstance.setServiceId(serviceId);
							serviceInstance.setHost(uri.getHost());
							serviceInstance.setPort(uri.getPort());
							enhancedPluginContext.setTargetServiceInstance(serviceInstance, null);
						}
						else {
							enhancedPluginContext.setTargetServiceInstance(null, uri);
						}
					}
				})
				.doOnSuccess(v -> {
					enhancedPluginContext.setDelay(System.currentTimeMillis() - startTime);
					EnhancedResponseContext enhancedResponseContext = EnhancedResponseContext.builder()
							.httpStatus(exchange.getResponse().getRawStatusCode())
							.httpHeaders(exchange.getResponse().getHeaders())
							.build();
					enhancedPluginContext.setResponse(enhancedResponseContext);

					// Run post enhanced plugins.
					pluginRunner.run(EnhancedPluginType.Client.POST, enhancedPluginContext);
				})
				.doOnError(t -> {
					enhancedPluginContext.setDelay(System.currentTimeMillis() - startTime);
					enhancedPluginContext.setThrowable(t);

					// Run exception enhanced plugins.
					pluginRunner.run(EnhancedPluginType.Client.EXCEPTION, enhancedPluginContext);
				})
				.doFinally(v -> {
					// Run finally enhanced plugins.
					pluginRunner.run(EnhancedPluginType.Client.FINALLY, enhancedPluginContext);
				});
	}

	@Override
	public int getOrder() {
		return OrderConstant.Client.Scg.ENHANCED_FILTER_ORDER;
	}

	private URI getServiceUri(ServerWebExchange originExchange, String serviceId) {
		URI uri = originExchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
		if (StringUtils.isEmpty(serviceId) || uri == null) {
			return null;
		}
		try {
			return new URI(originExchange.getRequest().getURI().getScheme(),
					serviceId, uri.getPath(),
					originExchange.getRequest().getURI().getRawQuery());
		}
		catch (Exception e) {
			return null;
		}
	}
}
