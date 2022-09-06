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

package com.tencent.cloud.rpc.enhancement.resttemplate;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

/**
 * Extract load balancer result from {@link LoadBalancerClient} and put to MetadataContext.
 * @author lepdou 2022-09-06
 */
public final class LoadBalancerClientAspectUtils {

	private LoadBalancerClientAspectUtils() {
	}

	public static void extractLoadBalancerResult(ProceedingJoinPoint joinPoint) {
		Object server = joinPoint.getArgs()[0];
		if (server instanceof ServiceInstance) {
			ServiceInstance instance = (ServiceInstance) server;
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_LOAD_BALANCER, "host", instance.getHost());
			MetadataContextHolder.get()
					.putContext(MetadataContext.FRAGMENT_LOAD_BALANCER, "port", String.valueOf(instance.getPort()));
		}
	}
}
