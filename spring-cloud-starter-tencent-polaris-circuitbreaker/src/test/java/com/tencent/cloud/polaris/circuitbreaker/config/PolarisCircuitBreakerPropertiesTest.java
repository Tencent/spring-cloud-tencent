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

package com.tencent.cloud.polaris.circuitbreaker.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisCircuitBreakerProperties}.
 *
 * @author Haotian Zhang
 */
public class PolarisCircuitBreakerPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.TestConfig.class))
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.enabled=true")
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.default-rule-enabled=false")
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.default-error-count=1")
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.default-error-percent=2")
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.default-interval=3")
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.default-minimum-request=4");

	@Test
	public void testDefaultInitialization() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(PolarisCircuitBreakerProperties.class);
			PolarisCircuitBreakerProperties polarisCircuitBreakerProperties = context.getBean(PolarisCircuitBreakerProperties.class);
			assertThat(polarisCircuitBreakerProperties.isDefaultRuleEnabled()).isFalse();
			assertThat(polarisCircuitBreakerProperties.getDefaultErrorCount()).isEqualTo(1);
			assertThat(polarisCircuitBreakerProperties.getDefaultErrorPercent()).isEqualTo(2);
			assertThat(polarisCircuitBreakerProperties.getDefaultInterval()).isEqualTo(3);
			assertThat(polarisCircuitBreakerProperties.getDefaultMinimumRequest()).isEqualTo(4);
		});
	}

	@EnableAutoConfiguration
	protected static class TestApplication {

		@Configuration
		@EnableConfigurationProperties(PolarisCircuitBreakerProperties.class)
		protected static class TestConfig {

		}
	}
}
