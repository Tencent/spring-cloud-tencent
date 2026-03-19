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

package com.tencent.cloud.polaris.eager.instrument.loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

/**
 * Utility class for load balancer warm-up operations.
 * Provides common warm-up logic for eager loading services.
 *
 * @author Yuwei Fu
 */
public final class LoadBalancerWarmUpUtils {

	private static final Logger LOG = LoggerFactory.getLogger(LoadBalancerWarmUpUtils.class);

	private LoadBalancerWarmUpUtils() {
	}

	/**
	 * Warm up a service by triggering load balancer initialization.
	 * @param factory the LoadBalancerClientFactory
	 * @param serviceName the service name to warm up
	 * @return true if warm-up succeeded, false otherwise
	 */
	public static boolean warmUp(LoadBalancerClientFactory factory, String serviceName) {
		try {
			ReactiveLoadBalancer<ServiceInstance> loadBalancer = factory.getInstance(serviceName);
			if (loadBalancer != null) {
				loadBalancer.choose();
				LOG.info("[{}] eager-load end", serviceName);
				return true;
			}
			else {
				LOG.warn("[{}] no loadBalancer found.", serviceName);
				return false;
			}
		}
		catch (Exception e) {
			LOG.debug("[{}] eager-load failed.", serviceName, e);
			return false;
		}
	}
}
