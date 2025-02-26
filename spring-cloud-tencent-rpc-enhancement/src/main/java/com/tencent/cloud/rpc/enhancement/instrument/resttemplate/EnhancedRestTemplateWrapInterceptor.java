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

package com.tencent.cloud.rpc.enhancement.instrument.resttemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.metadata.core.MetadataObjectValue;
import com.tencent.polaris.metadata.core.MetadataType;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.ServiceRequestWrapper;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import static com.tencent.cloud.rpc.enhancement.instrument.resttemplate.PolarisLoadBalancerRequestTransformer.LOAD_BALANCER_SERVICE_INSTANCE;

/**
 * EnhancedRestTemplateInterceptor.
 *
 * @author sean yu
 */
public class EnhancedRestTemplateWrapInterceptor {

	private final EnhancedPluginRunner pluginRunner;

	private final LoadBalancerClient delegate;

	public EnhancedRestTemplateWrapInterceptor(EnhancedPluginRunner pluginRunner, LoadBalancerClient delegate) {
		this.pluginRunner = pluginRunner;
		this.delegate = delegate;
	}


	public ClientHttpResponse intercept(HttpRequest request, String serviceId,
			LoadBalancerRequest<ClientHttpResponse> loadBalancerRequest) throws IOException {

		EnhancedPluginContext enhancedPluginContext = new EnhancedPluginContext();

		URI serviceUrl = request.getURI();
		if (request instanceof ServiceRequestWrapper) {
			serviceUrl = ((ServiceRequestWrapper) request).getRequest().getURI();
		}

		EnhancedRequestContext enhancedRequestContext = EnhancedRequestContext.builder()
				.httpHeaders(request.getHeaders())
				.httpMethod(request.getMethod())
				.url(request.getURI())
				.serviceUrl(serviceUrl)
				.build();
		enhancedPluginContext.setRequest(enhancedRequestContext);
		enhancedPluginContext.setOriginRequest(request);

		enhancedPluginContext.setLocalServiceInstance(pluginRunner.getLocalServiceInstance());


		// Run pre enhanced plugins.
		try {
			pluginRunner.run(EnhancedPluginType.Client.PRE, enhancedPluginContext);
		}
		catch (CallAbortedException callAbortedException) {
			MetadataObjectValue<Object> fallbackResponseValue = MetadataContextHolder.get().
					getMetadataContainer(MetadataType.APPLICATION, true).
					getMetadataValue(ContextConstant.CircuitBreaker.CIRCUIT_BREAKER_FALLBACK_HTTP_RESPONSE);

			boolean existFallback = Optional.ofNullable(fallbackResponseValue).
					map(MetadataObjectValue::getObjectValue).map(Optional::isPresent).orElse(false);

			if (existFallback) {
				Object fallbackResponse = fallbackResponseValue.getObjectValue().orElse(null);
				if (fallbackResponse instanceof ClientHttpResponse) {
					return (ClientHttpResponse) fallbackResponse;
				}
			}
			throw callAbortedException;
		}

		long startMillis = System.currentTimeMillis();
		try {
			ClientHttpResponse response = delegate.execute(serviceId, loadBalancerRequest);
			// get target instance after execute
			enhancedPluginContext.setTargetServiceInstance((ServiceInstance) MetadataContextHolder.get()
					.getLoadbalancerMetadata().get(LOAD_BALANCER_SERVICE_INSTANCE), request.getURI());
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);

			EnhancedResponseContext enhancedResponseContext = EnhancedResponseContext.builder()
					.httpStatus(response.getRawStatusCode())
					.httpHeaders(response.getHeaders())
					.build();
			enhancedPluginContext.setResponse(enhancedResponseContext);

			// Run post enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.POST, enhancedPluginContext);


			return response;
		}
		catch (IOException e) {
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);
			enhancedPluginContext.setThrowable(e);
			// Run exception enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.EXCEPTION, enhancedPluginContext);
			throw e;
		}
		finally {
			// Run finally enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.FINALLY, enhancedPluginContext);
		}
	}

}
