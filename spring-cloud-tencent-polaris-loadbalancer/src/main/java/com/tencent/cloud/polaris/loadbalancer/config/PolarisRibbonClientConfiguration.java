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

package com.tencent.cloud.polaris.loadbalancer.config;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.tencent.cloud.polaris.loadbalancer.PolarisLoadBalancer;
import com.tencent.polaris.api.core.ConsumerAPI;

import org.springframework.context.annotation.Bean;

/**
 * Configuration of ribbon client of Polaris.
 *
 * @author Haotian Zhang
 */
public class PolarisRibbonClientConfiguration {

	@Bean
	public ILoadBalancer polarisLoadBalancer(IClientConfig iClientConfig, IRule iRule,
			IPing iPing, ServerList<Server> serverList,
			ConsumerAPI consumerAPI, PolarisLoadBalancerProperties polarisLoadBalancerProperties) {
		return new PolarisLoadBalancer(iClientConfig, iRule, iPing, serverList,
				consumerAPI, polarisLoadBalancerProperties);
	}
}
