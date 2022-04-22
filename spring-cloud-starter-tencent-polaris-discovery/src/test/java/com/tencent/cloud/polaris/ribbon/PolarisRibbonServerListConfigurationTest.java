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

package com.tencent.cloud.polaris.ribbon;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.tencent.cloud.polaris.context.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClientConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisRibbonServerListConfiguration}
 *
 * @author Haotian Zhang
 */
public class PolarisRibbonServerListConfigurationTest {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PolarisRibbonClientTest.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisContextAutoConfiguration.class))
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues(
					"spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx");

	@Test
	public void testProperties() {
		this.contextRunner.run(context -> {
			PolarisDiscoveryHandler discoveryHandler = context
					.getBean(PolarisDiscoveryHandler.class);
			PolarisServerList serverList = new PolarisServerList(discoveryHandler);
			IClientConfig iClientConfig = context.getBean(IClientConfig.class);
			serverList.initWithNiwsConfig(iClientConfig);

			assertThat(serverList.getServiceId()).isEqualTo(SERVICE_PROVIDER);
		});
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableDiscoveryClient
	static class PolarisRibbonClientTest {

		@Bean
		IClientConfig iClientConfig() {
			DefaultClientConfigImpl config = new DefaultClientConfigImpl();
			config.setClientName(SERVICE_PROVIDER);
			return config;
		}

		@Bean
		@LoadBalanced
		RestTemplate restTemplate() {
			return new RestTemplate();
		}

	}

}
