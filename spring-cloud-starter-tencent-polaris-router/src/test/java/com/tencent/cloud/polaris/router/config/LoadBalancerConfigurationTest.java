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

import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.router.PolarisRouterServiceInstanceListSupplier;
import com.tencent.cloud.rpc.enhancement.transformer.PolarisInstanceTransformer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * test for {@link LoadBalancerConfiguration}.
 * @author dongyinuo
 */
public class LoadBalancerConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void testLoadBalancerConfiguration() {
		contextRunner.withConfiguration(AutoConfigurations.of(
						PolarisContextAutoConfiguration.class,
						LoadBalancerConfiguration.class))
				.run(context -> {
					assertThat(context).hasSingleBean(LoadBalancerConfiguration.class);
				});
	}

	@Test
	public void testPolarisReactiveSupportConfiguration() {
		contextRunner.withConfiguration(AutoConfigurations.of(
						LoadBalancerConfiguration.PolarisReactiveSupportConfiguration.class,
						PolarisContextAutoConfiguration.class))
				.withBean(SimpleReactiveDiscoveryProperties.class)
				.withBean(SimpleReactiveDiscoveryClient.class)
				.withBean(PolarisInstanceTransformer.class)
				.run(context -> {
					assertThat(context).hasSingleBean(LoadBalancerConfiguration.PolarisReactiveSupportConfiguration.class);
					assertThat(context).hasSingleBean(ReactiveDiscoveryClient.class);
					assertThat(context).hasSingleBean(PolarisRouterServiceInstanceListSupplier.class);
				});
	}

	@Test
	public void testPolarisBlockingSupportConfiguration() {
		contextRunner.withConfiguration(AutoConfigurations.of(
						PolarisContextAutoConfiguration.class,
						LoadBalancerConfiguration.PolarisBlockingSupportConfiguration.class
				))
				.withBean(SimpleDiscoveryProperties.class)
				.withBean(SimpleDiscoveryClient.class)
				.withBean(PolarisInstanceTransformer.class)
				.run(context -> {
					assertThat(context).hasSingleBean(LoadBalancerConfiguration.PolarisBlockingSupportConfiguration.class);
					assertThat(context).hasSingleBean(DiscoveryClient.class);
					assertThat(context).hasSingleBean(PolarisRouterServiceInstanceListSupplier.class);
				});
	}

}
