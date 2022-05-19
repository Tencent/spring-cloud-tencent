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

package com.tencent.cloud.polaris.router.config;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.tencent.cloud.polaris.router.feign.PolarisFeignLoadBalancer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * configuration for feign component.
 *
 *@author lepdou 2022-05-16
 */
@Configuration
public class FeignConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PolarisFeignLoadBalancer polarisFeignLoadBalancer(ILoadBalancer lb, IClientConfig clientConfig,
			ServerIntrospector serverIntrospector) {
		return new PolarisFeignLoadBalancer(lb, clientConfig, serverIntrospector);
	}
}
