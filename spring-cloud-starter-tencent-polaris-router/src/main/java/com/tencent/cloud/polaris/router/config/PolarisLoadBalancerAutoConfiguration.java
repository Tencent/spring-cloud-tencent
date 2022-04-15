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

package com.tencent.cloud.polaris.router.config;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.router.api.core.RouterAPI;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration of loadbalancer for Polaris.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConditionalOnPolarisEnabled
@ConditionalOnProperty(value = "spring.cloud.polaris.loadbalancer.enabled", matchIfMissing = true)
@AutoConfigureAfter(LoadBalancerAutoConfiguration.class)
@LoadBalancerClients(defaultConfiguration = PolarisLoadBalancerClientConfiguration.class)
public class PolarisLoadBalancerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PolarisLoadBalancerProperties polarisLoadBalancerProperties() {
		return new PolarisLoadBalancerProperties();
	}

	@Bean(name = "polarisRoute")
	@ConditionalOnMissingBean
	public RouterAPI polarisRouter(SDKContext polarisContext) throws PolarisException {
		return RouterAPIFactory.createRouterAPIByContext(polarisContext);
	}

}
