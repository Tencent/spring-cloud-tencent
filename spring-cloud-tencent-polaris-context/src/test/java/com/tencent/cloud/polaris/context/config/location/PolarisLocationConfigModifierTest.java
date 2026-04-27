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

package com.tencent.cloud.polaris.context.config.location;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.plugin.location.LocationProvider;
import com.tencent.polaris.factory.config.global.LocationConfigImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisLocationConfigModifier}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisLocationConfigModifier")
class PolarisLocationConfigModifierTest {

	private final ApplicationContextRunner enabledContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestApplication.class)
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.location.cloud.enabled=true")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.server.webflux.enabled=false");

	private final ApplicationContextRunner disabledContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestApplication.class)
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.location.cloud.enabled=false")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.server.webflux.enabled=false");

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	/**
	 * Test purpose: verify that cloud location provider is added by default.
	 * Test scenario: no explicit location config, cloud.enabled defaults to true.
	 * Verification: providers list contains one entry with type "cloud".
	 */
	@DisplayName("cloud provider is added when enabled by default")
	@Test
	void testModify_cloudEnabledByDefault() {
		enabledContextRunner.run(context -> {
			PolarisSDKContextManager manager = context.getBean(PolarisSDKContextManager.class);
			LocationConfigImpl locationConfig = (LocationConfigImpl) manager.getSDKContext()
					.getConfig().getGlobal().getLocation();
			boolean hasCloudProvider = locationConfig.getProviders().stream()
					.anyMatch(p -> LocationProvider.ProviderType.CLOUD.getName().equals(p.getType()));
			assertThat(hasCloudProvider).isTrue();
		});
	}

	/**
	 * Test purpose: verify that cloud location provider is removed when disabled.
	 * Test scenario: spring.cloud.polaris.location.cloud.enabled=false.
	 * Verification: providers list contains no entry with type "cloud".
	 */
	@DisplayName("cloud provider is removed when disabled")
	@Test
	void testModify_cloudDisabled() {
		disabledContextRunner.run(context -> {
			PolarisSDKContextManager manager = context.getBean(PolarisSDKContextManager.class);
			LocationConfigImpl locationConfig = (LocationConfigImpl) manager.getSDKContext()
					.getConfig().getGlobal().getLocation();
			boolean hasCloudProvider = locationConfig.getProviders().stream()
					.anyMatch(p -> LocationProvider.ProviderType.CLOUD.getName().equals(p.getType()));
			assertThat(hasCloudProvider).isFalse();
		});
	}

	/**
	 * Test purpose: verify that no duplicate cloud provider entry is added.
	 * Test scenario: cloud is enabled (default), Spring context is loaded once.
	 * Verification: providers list contains exactly one entry with type "cloud".
	 */
	@DisplayName("no duplicate cloud provider entry is added")
	@Test
	void testModify_noDuplicateCloudProvider() {
		enabledContextRunner.run(context -> {
			PolarisSDKContextManager manager = context.getBean(PolarisSDKContextManager.class);
			LocationConfigImpl locationConfig = (LocationConfigImpl) manager.getSDKContext()
					.getConfig().getGlobal().getLocation();
			long cloudProviderCount = locationConfig.getProviders().stream()
					.filter(p -> LocationProvider.ProviderType.CLOUD.getName().equals(p.getType()))
					.count();
			assertThat(cloudProviderCount).isEqualTo(1);
		});
	}

	/**
	 * Test purpose: verify getOrder returns the location order constant.
	 * Test scenario: obtain modifier bean from context and call getOrder.
	 * Verification: order equals OrderConstant.Modifier.LOCATION_ORDER.
	 */
	@DisplayName("getOrder returns LOCATION_ORDER")
	@Test
	void testGetOrder() {
		enabledContextRunner.run(context -> {
			PolarisLocationConfigModifier modifier = context.getBean(PolarisLocationConfigModifier.class);
			assertThat(modifier.getOrder()).isEqualTo(OrderConstant.Modifier.LOCATION_ORDER);
		});
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
