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

package com.tencent.cloud.polaris.registry;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link PolarisRegistrationCustomizer}.
 *
 * @author Haotian Zhang
 */
public class PolarisRegistrationCustomizerTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					PolarisServiceRegistryAutoConfiguration.class,
					PolarisDiscoveryClientConfiguration.class))
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + PORT)
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
			PolarisRegistrationCustomizer customizer = context.getBean(PolarisRegistrationCustomizer.class);
			verify(customizer, times(1)).customize(any(PolarisRegistration.class));
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
