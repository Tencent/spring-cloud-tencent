/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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
 *
 */

package com.tencent.cloud.polaris.router.resttemplate;

import java.io.IOException;
import java.net.URI;

import com.tencent.cloud.polaris.router.PolarisRouterContext;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * PolarisLoadBalancerInterceptor extends LoadBalancerInterceptor capabilities.
 * Parses the label from the request and puts it into the RouterContext for routing.
 *
 * @author lepdou 2022-05-18
 */
public class PolarisLoadBalancerInterceptor extends LoadBalancerInterceptor {

	private final LoadBalancerClient loadBalancer;
	private final LoadBalancerRequestFactory requestFactory;
	private final RouterContextFactory routerContextFactory;
	private final boolean isRibbonLoadBalanceClient;

	public PolarisLoadBalancerInterceptor(LoadBalancerClient loadBalancer,
			LoadBalancerRequestFactory requestFactory,
			RouterContextFactory routerContextFactory) {
		super(loadBalancer, requestFactory);
		this.loadBalancer = loadBalancer;
		this.requestFactory = requestFactory;
		this.routerContextFactory = routerContextFactory;

		this.isRibbonLoadBalanceClient = loadBalancer instanceof RibbonLoadBalancerClient;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		String peerServiceName = originalUri.getHost();
		Assert.state(peerServiceName != null,
				"Request URI does not contain a valid hostname: " + originalUri);

		if (isRibbonLoadBalanceClient) {
			//1. create router context
			PolarisRouterContext routerContext = routerContextFactory.create(request, body, peerServiceName);

			//2. set router context to request
			RouterContextHelper.setRouterContextToRequest(request, routerContext);

			//3. do loadbalancer and execute request
			ClientHttpResponse response = ((RibbonLoadBalancerClient) loadBalancer).execute(peerServiceName,
					this.requestFactory.createRequest(request, body, execution), routerContext);

			//4. set router context to response
			RouterContextHelper.setRouterContextToResponse(routerContext, response);
			return response;
		}

		return this.loadBalancer.execute(peerServiceName, this.requestFactory.createRequest(request, body, execution));
	}
}
