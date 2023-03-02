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

package com.tencent.cloud.polaris.router.endpoint;

import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * test for {@link PolarisRouterEndpointAutoConfiguration}.
 *
 * @author dongyinuo
 */
public class PolarisRouterEndpointAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					TestServiceRuleManagerConfiguration.class,
					PolarisRouterEndpointAutoConfiguration.class))
			.withPropertyValues("endpoints.polaris-router.enabled=true");
	private ServiceRuleManager serviceRuleManager;

	@Test
	public void polarisRouterEndpoint() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(PolarisRouterEndpointAutoConfiguration.class);
		});
	}

	@Configuration
	static class TestServiceRuleManagerConfiguration {

		@Bean
		public ServiceRuleManager serviceRuleManager(SDKContext sdkContext, ConsumerAPI consumerAPI) {
			return new ServiceRuleManager(sdkContext, consumerAPI);
		}

		@Bean
		public ConsumerAPI consumerAPI(SDKContext sdkContext) {
			return DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);
		}

		@Bean
		public SDKContext sdkContext() {
			return SDKContext.initContext();
		}
	}
}
