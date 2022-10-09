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
 */

package com.tencent.cloud.polaris.router.resttemplate;

import com.tencent.cloud.polaris.router.PolarisRouterContext;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.InterceptorRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryContext;

/**
 * Override InterceptorRetryPolicy for passing router context.
 *
 * @author lepdou 2022-10-09
 */
public class PolarisInterceptorRetryPolicy extends InterceptorRetryPolicy {

	private final HttpRequest request;

	private final LoadBalancedRetryPolicy policy;

	private final ServiceInstanceChooser serviceInstanceChooser;

	private final String serviceName;

	private final PolarisRouterContext routerContext;


	public PolarisInterceptorRetryPolicy(HttpRequest request, LoadBalancedRetryPolicy policy,
			ServiceInstanceChooser serviceInstanceChooser, String serviceName,
			PolarisRouterContext routerContext) {
		super(request, policy, serviceInstanceChooser, serviceName);

		this.request = request;
		this.policy = policy;
		this.serviceInstanceChooser = serviceInstanceChooser;
		this.serviceName = serviceName;
		this.routerContext = routerContext;
	}

	@Override
	public boolean canRetry(RetryContext context) {
		if (serviceInstanceChooser instanceof RibbonLoadBalancerClient) {
			LoadBalancedRetryContext lbContext = (LoadBalancedRetryContext) context;
			if (lbContext.getRetryCount() == 0 && lbContext.getServiceInstance() == null) {
				loadbalancerWithRouterContext(lbContext, routerContext);
				return true;
			}
			return policy.canRetryNextServer(lbContext);
		}
		else {
			return super.canRetry(context);
		}
	}

	private void loadbalancerWithRouterContext(LoadBalancedRetryContext lbContext, PolarisRouterContext routerContext) {
		RibbonLoadBalancerClient ribbonLoadBalancerClient = (RibbonLoadBalancerClient) serviceInstanceChooser;

		// set router context to request
		RouterContextHelper.setRouterContextToRequest(request, routerContext);

		ServiceInstance serviceInstance = ribbonLoadBalancerClient.choose(serviceName, request);
		lbContext.setServiceInstance(serviceInstance);
	}
}
