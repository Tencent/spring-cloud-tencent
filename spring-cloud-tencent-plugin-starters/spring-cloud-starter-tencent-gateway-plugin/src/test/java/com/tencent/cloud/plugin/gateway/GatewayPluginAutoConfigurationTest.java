/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.plugin.gateway;

import com.tencent.cloud.plugin.gateway.context.ContextGatewayFilterFactory;
import com.tencent.cloud.plugin.gateway.context.ContextGatewayProperties;
import com.tencent.cloud.plugin.gateway.context.ContextGatewayPropertiesManager;
import com.tencent.cloud.plugin.gateway.context.ContextPropertiesRouteDefinitionLocator;
import com.tencent.cloud.plugin.gateway.context.ContextRoutePredicateFactory;
import com.tencent.cloud.plugin.gateway.context.GatewayConfigChangeListener;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.listener.ConfigChangeEvent;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GatewayPluginAutoConfiguration}.
 */
class GatewayPluginAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					ConfigurationPropertiesAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class,
					PolarisContextAutoConfiguration.class,
					GatewayAutoConfiguration.class,
					GatewayPluginAutoConfiguration.class
			))
			.withPropertyValues(
					"spring.cloud.gateway.enabled=false", // not needed for this test
					"spring.cloud.tencent.plugin.scg.enabled=true",
					"spring.cloud.tencent.plugin.scg.context.enabled=true",
					"spring.cloud.tencent.gateway.routes.test_group.uri=lb://test-group"
			)
			.withUserConfiguration(MockPolarisClientsConfiguration.class);

	@Test
	void shouldCreateBeansWhenConditionsMet() {
		contextRunner.run(context -> {
			// Verify core beans
			assertThat(context).hasSingleBean(ContextGatewayFilterFactory.class);
			assertThat(context).hasSingleBean(ContextPropertiesRouteDefinitionLocator.class);
			assertThat(context).hasSingleBean(ContextRoutePredicateFactory.class);
			assertThat(context).hasSingleBean(ContextGatewayPropertiesManager.class);
			assertThat(context).hasSingleBean(GatewayRegistrationCustomizer.class);
			assertThat(context).hasSingleBean(GatewayConfigChangeListener.class);
			assertThat(context).hasSingleBean(PolarisReactiveLoadBalancerClientFilterBeanPostProcessor.class);


			GatewayPluginAutoConfiguration.ContextPluginConfiguration pluginConfiguration =
					context.getBean(GatewayPluginAutoConfiguration.ContextPluginConfiguration.class);
			assertThat(pluginConfiguration).hasFieldOrPropertyWithValue("commonEagerLoadEnabled", true)
					.hasFieldOrPropertyWithValue("gatewayEagerLoadEnabled", false);

			ContextGatewayProperties properties = context.getBean(ContextGatewayProperties.class);
			properties.setRoutes(properties.getRoutes());
			properties.setGroups(properties.getGroups());
			properties.toString();

			// test listener
			GatewayConfigChangeListener listener = context.getBean(GatewayConfigChangeListener.class);
			listener.onChangeTencentGatewayProperties(new ConfigChangeEvent(null, null));
			listener.onChangeGatewayConfigChangeListener(new ConfigChangeEvent(null, null));

		});
	}

	@Test
	void shouldEagerLoad() {
		contextRunner
				.withPropertyValues("spring.cloud.polaris.discovery.eager-load.gateway.enabled=true")
				.run(context -> {
					// Verify eager loading
					GatewayPluginAutoConfiguration.ContextPluginConfiguration pluginConfiguration = context.getBean(GatewayPluginAutoConfiguration.ContextPluginConfiguration.class);
					assertThat(pluginConfiguration).hasFieldOrPropertyWithValue("commonEagerLoadEnabled", true)
							.hasFieldOrPropertyWithValue("gatewayEagerLoadEnabled", true);
				});
	}

	@Test
	void shouldNotCreateBeansWhenPluginDisabled() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(GatewayPluginAutoConfiguration.class))
				.withPropertyValues("spring.cloud.tencent.plugin.scg.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(ContextGatewayFilterFactory.class));
	}

	@Test
	void shouldNotCreateContextBeansWhenContextPluginDisabled() {
		contextRunner
				.withPropertyValues("spring.cloud.tencent.plugin.scg.context.enabled=false")
				.run(context -> {
					assertThat(context).doesNotHaveBean(ContextGatewayFilterFactory.class);
					assertThat(context).doesNotHaveBean(ContextPropertiesRouteDefinitionLocator.class);
				});
	}

	@Configuration
	static class MockPolarisClientsConfiguration {
		@Bean
		PolarisDiscoveryClient polarisDiscoveryClient() {
			return mock(PolarisDiscoveryClient.class);
		}

		@Bean
		PolarisReactiveDiscoveryClient polarisReactiveDiscoveryClient() {
			return mock(PolarisReactiveDiscoveryClient.class);
		}

		@Bean
		PolarisConfigProperties polarisConfigProperties() {
			return new PolarisConfigProperties();
		}
	}
}
