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

package com.tencent.cloud.polaris.router.feign;

import java.util.Map;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.FeignLoadBalancer;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Extends CachingSpringLoadBalancerFactory to be able to create PolarisFeignLoadBalance.
 *
 *@author lepdou 2022-05-16
 */
public class PolarisCachingSpringLoadBalanceFactory extends CachingSpringLoadBalancerFactory {

	private final Map<String, FeignLoadBalancer> cache = new ConcurrentReferenceHashMap<>();

	public PolarisCachingSpringLoadBalanceFactory(SpringClientFactory factory) {
		super(factory);
	}

	public PolarisCachingSpringLoadBalanceFactory(SpringClientFactory factory,
			LoadBalancedRetryFactory loadBalancedRetryPolicyFactory) {
		super(factory, loadBalancedRetryPolicyFactory);
	}

	@Override
	public FeignLoadBalancer create(String clientName) {
		FeignLoadBalancer client = this.cache.get(clientName);
		if (client != null) {
			return client;
		}

		IClientConfig config = this.factory.getClientConfig(clientName);
		ILoadBalancer lb = this.factory.getLoadBalancer(clientName);
		ServerIntrospector serverIntrospector = this.factory.getInstance(clientName, ServerIntrospector.class);

		FeignLoadBalancer loadBalancer = new PolarisFeignLoadBalancer(lb, config, serverIntrospector);

		//There is a concurrency problem here.
		//When the concurrency is high, it may cause a service to create multiple FeignLoadBalancers.
		//But there is no concurrency control in CachingSpringLoadBalancerFactory,
		//so no locks will be added here for the time being
		cache.putIfAbsent(clientName, loadBalancer);

		return loadBalancer;
	}

}
