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

package com.tencent.cloud.polaris.discovery.reactive;

import com.tencent.cloud.polaris.discovery.PolarisServiceDiscovery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ConditionalOnDiscoveryHealthIndicatorEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicatorProperties;
import org.springframework.cloud.client.discovery.health.reactive.ReactiveDiscoveryClientHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reactive Discovery Client Configuration for Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng, youta
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnReactiveDiscoveryEnabled
@EnableConfigurationProperties(DiscoveryClientHealthIndicatorProperties.class)
public class PolarisReactiveDiscoveryClientConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PolarisReactiveDiscoveryClient polarisReactiveDiscoveryClient(
			PolarisServiceDiscovery polarisServiceDiscovery) {
		return new PolarisReactiveDiscoveryClient(polarisServiceDiscovery);
	}

	@Bean
	@ConditionalOnClass(name = "org.springframework.boot.actuate.health.ReactiveHealthIndicator")
	@ConditionalOnDiscoveryHealthIndicatorEnabled
	public ReactiveDiscoveryClientHealthIndicator polarisReactiveDiscoveryClientHealthIndicator(
			PolarisReactiveDiscoveryClient client, DiscoveryClientHealthIndicatorProperties properties) {
		return new ReactiveDiscoveryClientHealthIndicator(client, properties);
	}
}
