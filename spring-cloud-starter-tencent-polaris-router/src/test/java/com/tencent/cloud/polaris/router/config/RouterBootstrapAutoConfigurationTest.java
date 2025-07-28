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

package com.tencent.cloud.polaris.router.config;

import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.router.RouterConfigModifier;
import com.tencent.cloud.polaris.router.config.properties.PolarisNearByRouterProperties;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementPropertiesAutoConfiguration;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.router.nearby.NearbyRouterConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RouterBootstrapAutoConfigurationTest.
 *
 * @author sean yu
 */
public class RouterBootstrapAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					PolarisNearByRouterProperties.class,
					RpcEnhancementPropertiesAutoConfiguration.class,
					RpcEnhancementAutoConfiguration.class,
					RouterBootstrapAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.router.nearby-router.matchLevel=CAMPUS")
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.enabled=true");

	@Test
	public void testDefaultInitialization() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(RouterConfigModifier.class);
			RouterConfigModifier routerConfigModifier = (RouterConfigModifier) context.getBean("routerConfigModifier");
			Configuration configuration = ConfigAPIFactory.defaultConfig();
			routerConfigModifier.modify((ConfigurationImpl) configuration);
			NearbyRouterConfig nearbyRouterConfig = configuration.getConsumer().getServiceRouter().getPluginConfig(
					ServiceRouterConfig.DEFAULT_ROUTER_NEARBY, NearbyRouterConfig.class);
			Assertions.assertEquals("CAMPUS", nearbyRouterConfig.getMatchLevel().name());
		});
	}
}
