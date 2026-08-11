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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.cloud.polaris.config.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisConfigProperties}.
 */
public class PolarisConfigPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	public void testReportEnabledDefaultTrue() {
		this.contextRunner.run(context -> {
			PolarisConfigProperties properties = context.getBean(PolarisConfigProperties.class);
			assertThat(properties.isReportEnabled()).isTrue();
		});
	}

	@Test
	public void testReportEnabledBinding() {
		this.contextRunner
				.withPropertyValues("spring.cloud.polaris.config.report.enabled=false")
				.run(context -> {
					PolarisConfigProperties properties = context.getBean(PolarisConfigProperties.class);
					assertThat(properties.isReportEnabled()).isFalse();
				});
	}

	@EnableConfigurationProperties(PolarisConfigProperties.class)
	static class TestConfiguration {
	}
}
