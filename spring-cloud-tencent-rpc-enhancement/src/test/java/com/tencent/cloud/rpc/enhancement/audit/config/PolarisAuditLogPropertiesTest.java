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

package com.tencent.cloud.rpc.enhancement.audit.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisAuditLogProperties}.
 *
 * @author Yuwei Fu
 */
public class PolarisAuditLogPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PolarisAuditLogPropertiesAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.enabled=true");

	@Test
	void testDefaultValues() {
		contextRunner.run(context -> {
			PolarisAuditLogProperties properties = context.getBean(PolarisAuditLogProperties.class);
			assertThat(properties.isEnabled()).isFalse();
			assertThat(properties.getFormat()).isEqualTo("json");
		});
	}

	@Test
	void testConfiguredValues() {
		contextRunner
				.withPropertyValues("spring.cloud.polaris.audit-log.enabled=true")
				.withPropertyValues("spring.cloud.polaris.audit-log.format=json")
				.run(context -> {
					PolarisAuditLogProperties properties = context.getBean(PolarisAuditLogProperties.class);
					assertThat(properties.isEnabled()).isTrue();
					assertThat(properties.getFormat()).isEqualTo("json");
				});
	}
}
