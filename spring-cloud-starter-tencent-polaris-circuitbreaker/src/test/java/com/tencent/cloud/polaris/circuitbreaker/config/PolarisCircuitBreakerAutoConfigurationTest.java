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

package com.tencent.cloud.polaris.circuitbreaker.config;

import com.tencent.cloud.polaris.circuitbreaker.common.CircuitBreakerConfigModifier;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementAutoConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisCircuitBreakerAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
public class PolarisCircuitBreakerAutoConfigurationTest {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					RpcEnhancementAutoConfiguration.class,
					LoadBalancerAutoConfiguration.class,
					PolarisCircuitBreakerFeignClientAutoConfiguration.class,
					PolarisCircuitBreakerAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.enabled=true");

	private final ApplicationContextRunner reactiveContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					RpcEnhancementAutoConfiguration.class,
					LoadBalancerAutoConfiguration.class,
					ReactivePolarisCircuitBreakerAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.enabled=true");

	@Test
	public void testDefaultInitialization() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(PolarisCircuitBreakerAutoConfiguration.class);
			assertThat(context).hasSingleBean(CircuitBreakerFactory.class);
			assertThat(context).hasSingleBean(CircuitBreakerConfigModifier.class);
			assertThat(context).hasSingleBean(CircuitBreakerNameResolver.class);
		});
	}

	@Test
	public void testReactiveInitialization() {
		this.reactiveContextRunner.run(context -> {
			assertThat(context).hasSingleBean(ReactivePolarisCircuitBreakerAutoConfiguration.class);
			assertThat(context).hasSingleBean(ReactiveCircuitBreakerFactory.class);
			assertThat(context).hasSingleBean(CircuitBreakerConfigModifier.class);
		});
	}

}
