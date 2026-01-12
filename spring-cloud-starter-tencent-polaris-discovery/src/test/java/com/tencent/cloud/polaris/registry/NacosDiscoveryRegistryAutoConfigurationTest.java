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

package com.tencent.cloud.polaris.registry;

import com.tencent.cloud.polaris.DiscoveryPropertiesAutoConfiguration;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.extend.nacos.NacosContextProperties;
import com.tencent.cloud.polaris.registry.nacos.NacosDiscoveryRegistryAutoConfiguration;
import com.tencent.cloud.polaris.registry.nacos.NacosPolarisRegistrationCustomizer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link NacosDiscoveryRegistryAutoConfiguration}.
 *
 * @author Yuwei Fu
 */
public class NacosDiscoveryRegistryAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					NacosDiscoveryRegistryAutoConfiguration.class,
					DiscoveryPropertiesAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.enabled=true");

	@Test
	public void testDefaultInitialization() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(NacosDiscoveryRegistryAutoConfiguration.class);
			assertThat(context).hasSingleBean(NacosPolarisRegistrationCustomizer.class);
			assertThat(context).hasSingleBean(NacosContextProperties.class);
		});
	}

	@Test
	public void testPolarisDisabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						PolarisContextAutoConfiguration.class,
						NacosDiscoveryRegistryAutoConfiguration.class,
						DiscoveryPropertiesAutoConfiguration.class))
				.withPropertyValues("spring.cloud.polaris.enabled=false")
				.run(context -> {
					assertThat(context).doesNotHaveBean(NacosDiscoveryRegistryAutoConfiguration.class);
					assertThat(context).doesNotHaveBean(NacosPolarisRegistrationCustomizer.class);
				});
	}
}
