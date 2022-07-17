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

import java.util.List;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.IRule;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.cloud.polaris.router.PolarisLoadBalancerCompositeRule;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.cloud.polaris.router.spi.RouterResponseInterceptor;
import com.tencent.polaris.router.api.core.RouterAPI;

import org.springframework.context.annotation.Bean;

/**
 * Configuration for ribbon components. IRule is not singleton bean, Each service corresponds to an IRule.
 *
 * @author lepdou 2022-05-17
 */
public class RibbonConfiguration {

	@Bean
	public IRule polarisLoadBalancerCompositeRule(RouterAPI routerAPI,
			PolarisLoadBalancerProperties polarisLoadBalancerProperties,
			IClientConfig iClientConfig, List<RouterRequestInterceptor> requestInterceptors,
			List<RouterResponseInterceptor> responseInterceptors) {
		return new PolarisLoadBalancerCompositeRule(routerAPI, polarisLoadBalancerProperties, iClientConfig,
				requestInterceptors, responseInterceptors);
	}
}
