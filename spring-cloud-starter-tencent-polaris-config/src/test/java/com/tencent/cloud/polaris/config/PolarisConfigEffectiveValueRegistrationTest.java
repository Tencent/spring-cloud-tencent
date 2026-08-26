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

package com.tencent.cloud.polaris.config;

import com.tencent.cloud.polaris.config.adapter.SpringConfigEffectiveValueProvider;
import com.tencent.polaris.configuration.api.core.ConfigEffectiveValueProvider;
import com.tencent.polaris.configuration.api.core.ConfigEffectiveValueRegistration;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.EffectiveValue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisConfigEffectiveValueAutoConfiguration}. The registration bean must
 * only be created in the main application context: in legacy bootstrap mode the bootstrap
 * context's Environment carries only bootstrap.yml sources, so a provider registered there
 * would report stale effective values.
 *
 * @author evelynwei
 */
// The mocked AutoCloseable registrations are never closed; the real bean is closed via destroyMethod.
@SuppressWarnings("try")
class PolarisConfigEffectiveValueRegistrationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PolarisConfigEffectiveValueAutoConfiguration.class));

	@Test
	void testRegistrationBeanCreatedWhenConfigFileServicePresent() {
		ConfigFileService configFileService = mock(ConfigFileService.class);
		ConfigEffectiveValueRegistration registration = mock(ConfigEffectiveValueRegistration.class);
		when(configFileService.registerEffectiveValueProvider(any(ConfigEffectiveValueProvider.class)))
				.thenReturn(registration);

		runner.withBean(ConfigFileService.class, () -> configFileService)
				.run(context -> {
					assertThat(context).hasSingleBean(ConfigEffectiveValueRegistration.class);
					verify(configFileService).registerEffectiveValueProvider(
							any(SpringConfigEffectiveValueProvider.class));
				});
	}

	@Test
	void testRegistrationBeanSkippedWhenConfigFileServiceAbsent() {
		// local data source etc.: no ConfigFileService bean, provider must not be registered
		runner.run(context -> assertThat(context).doesNotHaveBean(ConfigEffectiveValueRegistration.class));
	}

	@Test
	void testRegistrationBeanSkippedWhenExplicitlyDisabled() {
		ConfigFileService configFileService = mock(ConfigFileService.class);
		runner.withBean(ConfigFileService.class, () -> configFileService)
				.withPropertyValues("spring.cloud.polaris.config.report.effective.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(ConfigEffectiveValueRegistration.class));
	}

	@Test
	void testProviderResolvesAgainstRegisteringContextEnvironment() {
		ConfigFileService configFileService = mock(ConfigFileService.class);
		ConfigEffectiveValueRegistration registration = mock(ConfigEffectiveValueRegistration.class);
		ArgumentCaptor<ConfigEffectiveValueProvider> providerCaptor =
				ArgumentCaptor.forClass(ConfigEffectiveValueProvider.class);
		when(configFileService.registerEffectiveValueProvider(providerCaptor.capture()))
				.thenReturn(registration);

		runner.withBean(ConfigFileService.class, () -> configFileService)
				.withPropertyValues("sct.test.key=from-main-environment")
				.run(context -> {
					EffectiveValue value = providerCaptor.getValue().resolve("sct.test.key", null);
					assertThat(value.getEffectiveValue()).isEqualTo("from-main-environment");
				});
	}
}
