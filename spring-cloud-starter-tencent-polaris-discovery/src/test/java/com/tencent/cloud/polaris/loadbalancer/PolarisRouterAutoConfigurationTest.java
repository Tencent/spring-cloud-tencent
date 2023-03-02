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

package com.tencent.cloud.polaris.loadbalancer;

import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.polaris.router.api.core.RouterAPI;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisLoadBalancerAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
public class PolarisRouterAutoConfigurationTest {

	private final ApplicationContextRunner blockingContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisLoadBalancerTest.class,
					PolarisContextAutoConfiguration.class,
					PolarisLoadBalancerAutoConfiguration.class))
			.withPropertyValues("spring.cloud.loadbalancer.configurations=polaris");

	private final ApplicationContextRunner noPolarisContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisLoadBalancerTest.class,
					PolarisContextAutoConfiguration.class,
					PolarisLoadBalancerAutoConfiguration.class));

	/**
	 * Test for BlockingDiscovery.
	 */
	@Test
	public void test1() {
		this.blockingContextRunner.run(context -> {
			assertThat(context).hasSingleBean(RouterAPI.class);
		});
	}

	/**
	 * Test for no Polaris.
	 */
	@Test
	public void test2() {
		this.noPolarisContextRunner.run(context -> {
			assertThat(context).hasSingleBean(RouterAPI.class);
		});
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisLoadBalancerTest {

	}
}
