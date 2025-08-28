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

import java.util.Map;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClientConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PolarisRegistrationCustomizer}.
 *
 * @author Haotian Zhang
 */
public class NacosPolarisRegistrationCustomizerTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					PolarisServiceRegistryAutoConfiguration.class,
					PolarisDiscoveryClientConfiguration.class))
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + PORT)
			.withPropertyValues("spring.cloud.nacos.discovery.cluster-name=nacos")
			.withPropertyValues("spring.cloud.nacos.discovery.group=nacos")
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081");

	@BeforeEach
	public void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	public void testCustomize() {
		this.contextRunner.run(context -> {
			PolarisRegistration polarisRegistration = context.getBean(PolarisRegistration.class);
			polarisRegistration.customize();
			Map<String, String> metadata = polarisRegistration.getMetadata();
			assertThat(metadata.get("nacos.cluster")).isEqualTo("nacos");
			assertThat(metadata.get("nacos.group")).isEqualTo("nacos");
		});
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisServiceRegistryAutoConfiguration {
		@Bean
		public PolarisRegistrationCustomizer polarisRegistrationCustomizer() {
			return mock(PolarisRegistrationCustomizer.class);
		}
	}
}
