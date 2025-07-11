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

import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUtils;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.http.HttpRequest;

/**
 * PolarisBlockingLoadBalancerClient is a wrapper of BlockingLoadBalancerClient.
 *
 * @author Shedfree Wu
 */
public class PolarisBlockingLoadBalancerClient extends BlockingLoadBalancerClient {

	private final EnhancedPluginRunner enhancedPluginRunner;
	private BlockingLoadBalancerClient delegate;

	public PolarisBlockingLoadBalancerClient(ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory,
			BlockingLoadBalancerClient delegate, EnhancedPluginRunner enhancedPluginRunner) {
		super(loadBalancerClientFactory);
		this.delegate = delegate;
		this.enhancedPluginRunner = enhancedPluginRunner;
	}

	/**
	 * common rest template.
	 */
	@Override
	public <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException {
		HttpRequest httpRequest = LoadBalancerUtils.getHttpRequestIfAvailable(request);
		if (httpRequest == null || enhancedPluginRunner == null) {
			return delegate.execute(serviceId, request);
		}
		byte[] body = LoadBalancerUtils.getHttpBodyIfAvailable(request);
		EnhancedRestTemplateWrapInterceptor enhancedRestTemplateWrapInterceptor = new EnhancedRestTemplateWrapInterceptor(enhancedPluginRunner, delegate);
		return enhancedRestTemplateWrapInterceptor.intercept(httpRequest, body, serviceId, null, request);
	}

	/**
	 * retry rest template.
	 */
	@Override
	public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException {
		HttpRequest httpRequest = LoadBalancerUtils.getHttpRequestIfAvailable(LoadBalancerUtils.getDelegateLoadBalancerRequestIfAvailable(request));
		if (httpRequest == null || serviceInstance == null || enhancedPluginRunner == null) {
			return delegate.execute(serviceId, serviceInstance, request);
		}
		byte[] body = LoadBalancerUtils.getHttpBodyIfAvailable(request);
		EnhancedRestTemplateWrapInterceptor enhancedRestTemplateWrapInterceptor = new EnhancedRestTemplateWrapInterceptor(enhancedPluginRunner, delegate);
		return enhancedRestTemplateWrapInterceptor.intercept(httpRequest, body, serviceId, serviceInstance, request);
	}
}
