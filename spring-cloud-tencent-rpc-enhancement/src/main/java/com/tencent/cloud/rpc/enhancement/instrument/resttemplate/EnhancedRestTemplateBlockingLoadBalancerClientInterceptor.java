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

package com.tencent.cloud.rpc.enhancement.instrument.resttemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import com.tencent.cloud.rpc.enhancement.util.EnhancedPluginUtils;
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
 * Interceptor used for pre-plugin, post-plugin, exception-plugin, and final-plugin in RestTemplate.
 *
 * @author sean yu
 */
public class EnhancedRestTemplateBlockingLoadBalancerClientInterceptor {

	private final EnhancedPluginRunner pluginRunner;

	private final LoadBalancerClient delegate;

	public EnhancedRestTemplateBlockingLoadBalancerClientInterceptor(EnhancedPluginRunner pluginRunner, LoadBalancerClient delegate) {
		this.pluginRunner = pluginRunner;
		this.delegate = delegate;
	}


	public <T> T intercept(HttpRequest httpRequest, byte[] body, String serviceId, ServiceInstance serviceInstance,
			LoadBalancerRequest<T> loadBalancerRequest) throws IOException {

		EnhancedPluginContext enhancedPluginContext = EnhancedPluginUtils.createEnhancedPluginContext();

		URI serviceUrl = httpRequest.getURI();
		if (httpRequest instanceof ServiceRequestWrapper) {
			serviceUrl = ((ServiceRequestWrapper) httpRequest).getRequest().getURI();
		}

		String governanceNamespace = MetadataContextHolder.get().getContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
				MetadataConstant.POLARIS_TARGET_NAMESPACE, MetadataContext.LOCAL_NAMESPACE);

		EnhancedRequestContext enhancedRequestContext = EnhancedRequestContext.builder()
				.httpHeaders(httpRequest.getHeaders())
				.httpMethod(httpRequest.getMethod())
				.url(httpRequest.getURI())
				.serviceUrl(serviceUrl)
				.governanceNamespace(governanceNamespace)
				.build();
		enhancedPluginContext.setRequest(enhancedRequestContext);
		enhancedPluginContext.setOriginRequest(httpRequest);
		if (body != null) {
			enhancedPluginContext.setOriginBody(body);
		}

		enhancedPluginContext.setLocalServiceInstance(pluginRunner.getLocalServiceInstance());

		long startMillis = System.currentTimeMillis();
		try {
			// Run pre enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.PRE, enhancedPluginContext);
			startMillis = System.currentTimeMillis();

			T response = null;
			// retry rest template, serviceInstance is not null
			if (serviceInstance != null) {
				response = delegate.execute(serviceId, serviceInstance, loadBalancerRequest);
			}
			else {
				response = delegate.execute(serviceId, loadBalancerRequest);
			}
			// get target instance after execute
			enhancedPluginContext.setTargetServiceInstance((ServiceInstance) MetadataContextHolder.get()
					.getLoadbalancerMetadata().get(LOAD_BALANCER_SERVICE_INSTANCE), httpRequest.getURI());
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);


			EnhancedResponseContext.EnhancedContextResponseBuilder enhancedResponseContextBuilder = EnhancedResponseContext.builder();
			if (response instanceof ClientHttpResponse) {
				enhancedResponseContextBuilder.httpStatus(((ClientHttpResponse) response).getStatusCode().value());
				enhancedResponseContextBuilder.httpHeaders(((ClientHttpResponse) response).getHeaders());
			}
			EnhancedResponseContext enhancedResponseContext = enhancedResponseContextBuilder.build();

			enhancedPluginContext.setResponse(enhancedResponseContext);

			// Run post enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.POST, enhancedPluginContext);


			return response;
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
					return (T) fallbackResponse;
				}
			}
			throw callAbortedException;
		}
		catch (IOException e) {
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);
			enhancedPluginContext.setThrowable(e);
			enhancedPluginContext.setTargetServiceInstance((ServiceInstance) MetadataContextHolder.get()
					.getLoadbalancerMetadata().get(LOAD_BALANCER_SERVICE_INSTANCE), httpRequest.getURI());
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
