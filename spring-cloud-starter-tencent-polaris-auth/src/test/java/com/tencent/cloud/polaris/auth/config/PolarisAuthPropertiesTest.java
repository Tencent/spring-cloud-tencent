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

package com.tencent.cloud.polaris.auth.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisAuthProperties}.
 *
 * @author Haotian Zhang
 */
public class PolarisAuthPropertiesTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PolarisAuthPropertiesAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.auth.enabled=false");

	@Test
	public void testGetAndSet() {
		this.applicationContextRunner.run(context -> {
			PolarisAuthProperties properties = context.getBean(PolarisAuthProperties.class);
			assertThat(properties.isEnabled()).isFalse();
		});
	}
}
