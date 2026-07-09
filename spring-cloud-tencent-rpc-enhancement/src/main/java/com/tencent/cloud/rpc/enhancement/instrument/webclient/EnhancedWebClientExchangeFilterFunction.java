/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.rpc.enhancement.instrument.webclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import com.tencent.cloud.rpc.enhancement.util.EnhancedPluginUtils;
import com.tencent.cloud.rpc.enhancement.util.OtelBaggageScopeHelper;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.fault.client.exception.FaultInjectionException;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static com.tencent.cloud.rpc.enhancement.instrument.resttemplate.PolarisLoadBalancerRequestTransformer.LOAD_BALANCER_SERVICE_INSTANCE;

/**
 * EnhancedWebClientExchangeFilterFunction.
 *
 * @author sean yu
 */
public class EnhancedWebClientExchangeFilterFunction implements ExchangeFilterFunction {
	private final EnhancedPluginRunner pluginRunner;

	public EnhancedWebClientExchangeFilterFunction(EnhancedPluginRunner pluginRunner) {
		this.pluginRunner = pluginRunner;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest originRequest, ExchangeFunction next) {
		EnhancedPluginContext enhancedPluginContext = EnhancedPluginUtils.createEnhancedPluginContext();

		String governanceNamespace = MetadataContextHolder.get()
				.getContextWithDefault(MetadataContext.FRAGMENT_APPLICATION_NONE, MetadataConstant.POLARIS_TARGET_NAMESPACE, MetadataContext.LOCAL_NAMESPACE);

		EnhancedRequestContext enhancedRequestContext = EnhancedRequestContext.builder()
				.httpHeaders(originRequest.headers())
				.httpMethod(originRequest.method())
				.url(originRequest.url())
				.serviceUrl(getServiceUri(originRequest))
				.governanceNamespace(governanceNamespace)
				.build();
		enhancedPluginContext.setRequest(enhancedRequestContext);
		enhancedPluginContext.setOriginRequest(originRequest);

		enhancedPluginContext.setLocalServiceInstance(pluginRunner.getLocalServiceInstance());
		enhancedPluginContext.setTargetServiceInstance((ServiceInstance) MetadataContextHolder.get()
				.getLoadbalancerMetadata().get(LOAD_BALANCER_SERVICE_INSTANCE), originRequest.url());

		long startTime = System.currentTimeMillis();
		// Run post enhanced plugins.
		try {
			pluginRunner.run(EnhancedPluginType.Client.PRE, enhancedPluginContext);
			pluginRunner.run(EnhancedPluginType.Client.BEFORE_CALLING, enhancedPluginContext);
		}
		catch (CallAbortedException e) {
			// Run finally enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.FINALLY, enhancedPluginContext);

			if (e.getFallbackInfo() == null) {
				throw e;
			}
			HttpStatus httpStatus = HttpStatus.resolve(e.getFallbackInfo().getCode());
			ClientResponse.Builder responseBuilder = ClientResponse.create(httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Optional.of(e.getFallbackInfo().getBody()).orElse(""));
			if (CollectionUtils.isNotEmpty(e.getFallbackInfo().getHeaders())) {
				e.getFallbackInfo().getHeaders().forEach(responseBuilder::header);
			}
			return Mono.just(responseBuilder.build());
		}
		catch (FaultInjectionException faultInjectionException) {
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startTime);
			if (faultInjectionException.getFallbackInfo() != null) {
				HttpStatus httpStatus = HttpStatus.resolve(faultInjectionException.getFallbackInfo().getCode());
				ClientResponse.Builder responseBuilder = ClientResponse.create(httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR)
						.body(Optional.of(faultInjectionException.getFallbackInfo().getBody()).orElse(""));
				if (CollectionUtils.isNotEmpty(faultInjectionException.getFallbackInfo().getHeaders())) {
					faultInjectionException.getFallbackInfo().getHeaders().forEach(responseBuilder::header);
				}
				ClientResponse response = responseBuilder.build();

				EnhancedResponseContext enhancedResponseContext = EnhancedResponseContext.builder()
						.httpStatus(response.statusCode().value())
						.httpHeaders(response.headers().asHttpHeaders())
						.build();
				enhancedPluginContext.setResponse(enhancedResponseContext);

				// Run post enhanced plugins.
				pluginRunner.run(EnhancedPluginType.Client.POST, enhancedPluginContext);

				pluginRunner.run(EnhancedPluginType.Client.FINALLY, enhancedPluginContext);

				return Mono.just(response);
			}
			else {
				enhancedPluginContext.setThrowable(faultInjectionException);

				// Run exception enhanced plugins.
				pluginRunner.run(EnhancedPluginType.Client.EXCEPTION, enhancedPluginContext);

				pluginRunner.run(EnhancedPluginType.Client.FINALLY, enhancedPluginContext);
				throw faultInjectionException;
			}
		}
		// request may be changed by plugin
		ClientRequest request = (ClientRequest) enhancedPluginContext.getOriginRequest();
		// Inject the baggage attributes staged in the pre stage as a W3C baggage header on the
		// downstream request. This avoids any dependency on the OTel Context ThreadLocal lifecycle
		// and prevents Scope.close from failing across async boundaries.
		ClientRequest downstreamRequest = injectPendingBaggageHeader(request, enhancedPluginContext);
		return next.exchange(downstreamRequest)
				.doOnSuccess(response -> {
					enhancedPluginContext.setDelay(System.currentTimeMillis() - startTime);

					EnhancedResponseContext enhancedResponseContext = EnhancedResponseContext.builder()
							.httpStatus(response.statusCode().value())
							.httpHeaders(response.headers().asHttpHeaders())
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

	/**
	 * Inject pending baggage attributes into the downstream ClientRequest as a W3C baggage HTTP
	 * header. This avoids any dependency on the OTel Context ThreadLocal lifecycle and prevents
	 * Scope.close from failing across async boundaries.
	 *
	 * @param request original ClientRequest
	 * @param ctx enhanced plugin context (holds the baggage attributes to write)
	 * @return the original request when there is nothing to write, otherwise a mutated ClientRequest
	 */
	private ClientRequest injectPendingBaggageHeader(ClientRequest request, EnhancedPluginContext ctx) {
		String existing = request.headers().getFirst(OtelBaggageScopeHelper.BAGGAGE_HEADER);
		String merged = OtelBaggageScopeHelper.resolvePendingBaggageHeader(ctx, existing);
		if (merged == null) {
			return request;
		}
		return ClientRequest.from(request)
				.headers(headers -> headers.set(OtelBaggageScopeHelper.BAGGAGE_HEADER, merged))
				.build();
	}

	private URI getServiceUri(ClientRequest clientRequest) {
		Object instance = MetadataContextHolder.get()
				.getLoadbalancerMetadata().get(LOAD_BALANCER_SERVICE_INSTANCE);
		if (!(instance instanceof ServiceInstance)) {
			return null;
		}
		ServiceInstance serviceInstance = (ServiceInstance) instance;
		URI uri = clientRequest.url();

		try {
			return new URI(uri.getScheme(), serviceInstance.getServiceId(), uri.getPath(), uri.getRawQuery());
		}
		catch (URISyntaxException e) {
			return null;
		}
	}
}
