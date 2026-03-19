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

package com.tencent.cloud.polaris.eager.config;

import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClient;
import com.tencent.cloud.polaris.eager.instrument.feign.FeignEagerLoadContextInitializer;
import com.tencent.cloud.polaris.eager.instrument.loadbalancer.LoadBalancerEagerContextInitializer;
import com.tencent.cloud.polaris.eager.instrument.loadbalancer.LoadBalancerEagerLoadProperties;
import com.tencent.cloud.polaris.eager.instrument.services.ServicesEagerLoadSmartLifecycle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LoadBalancerEagerLoadProperties.class)
@ConditionalOnProperty(name = "spring.cloud.polaris.discovery.eager-load.enabled", havingValue = "true", matchIfMissing = true)
public class PolarisEagerLoadAutoConfiguration {

	@Bean
	@ConditionalOnClass(name = "feign.Feign")
	@ConditionalOnProperty(name = "spring.cloud.polaris.discovery.eager-load.feign.enabled", havingValue = "true", matchIfMissing = true)
	public FeignEagerLoadContextInitializer feignEagerLoadContextInitializer(
			ApplicationContext applicationContext,
			LoadBalancerClientFactory loadBalancerClientFactory,
			LoadBalancerEagerLoadProperties loadBalancerEagerLoadProperties) {
		return new FeignEagerLoadContextInitializer(applicationContext, loadBalancerClientFactory, loadBalancerEagerLoadProperties);
	}
	@Bean
	@ConditionalOnProperty(name = "spring.cloud.polaris.discovery.eager-load.services.enabled", havingValue = "true", matchIfMissing = true)
	public ServicesEagerLoadSmartLifecycle serviceEagerLoadSmartLifecycle(
			@Autowired(required = false) PolarisDiscoveryClient polarisDiscoveryClient,
			@Autowired(required = false) PolarisReactiveDiscoveryClient polarisReactiveDiscoveryClient) {
		return new ServicesEagerLoadSmartLifecycle(polarisDiscoveryClient, polarisReactiveDiscoveryClient);
	}

	@Bean
	@ConditionalOnProperty(value = "spring.cloud.loadbalancer.eager-load.enabled", matchIfMissing = true)
	public LoadBalancerEagerContextInitializer loadBalancerEagerContextInitializer(
			LoadBalancerClientFactory loadBalancerClientFactory, LoadBalancerEagerLoadProperties properties) {
		return new LoadBalancerEagerContextInitializer(loadBalancerClientFactory, properties.getClients());
	}
}

