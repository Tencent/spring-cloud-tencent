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

package com.tencent.cloud.polaris.circuitbreaker.instrument.resttemplate;

import java.io.IOException;
import java.net.URI;

import com.tencent.cloud.rpc.enhancement.instrument.resttemplate.EnhancedRestTemplateWrapInterceptor;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * PolarisLoadBalancerInterceptor is a wrapper of LoadBalancerInterceptor.
 *
 * @author Shedfree Wu
 */
public class PolarisLoadBalancerInterceptor extends LoadBalancerInterceptor {

	private final LoadBalancerClient loadBalancer;
	private final LoadBalancerRequestFactory requestFactory;

	private final EnhancedPluginRunner enhancedPluginRunner;

	public PolarisLoadBalancerInterceptor(LoadBalancerClient loadBalancer, LoadBalancerRequestFactory requestFactory,
			EnhancedPluginRunner enhancedPluginRunner) {
		super(loadBalancer, requestFactory);
		this.loadBalancer = loadBalancer;
		this.requestFactory = requestFactory;
		this.enhancedPluginRunner = enhancedPluginRunner;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		String peerServiceName = originalUri.getHost();
		Assert.state(peerServiceName != null,
				"Request URI does not contain a valid hostname: " + originalUri);

		if (enhancedPluginRunner != null) {
			EnhancedRestTemplateWrapInterceptor enhancedRestTemplateWrapInterceptor = new EnhancedRestTemplateWrapInterceptor(enhancedPluginRunner, loadBalancer);
			return enhancedRestTemplateWrapInterceptor.intercept(request, peerServiceName, this.requestFactory.createRequest(request, body, execution));
		}
		else {
			return super.intercept(request, body, execution);
		}
	}
}
