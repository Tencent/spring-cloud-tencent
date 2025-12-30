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

package com.tencent.cloud.plugin.mq.lane.kafka;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.cloud.plugin.mq.lane.tsf.TsfActiveLane;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link KafkaLaneAspectConfiguration}.
 */
public class KafkaLaneAspectConfigurationTest {

	private ApplicationContextRunner contextRunner;

	@BeforeEach
	public void setUp() {
		contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(KafkaLaneAspectConfiguration.class))
				.withUserConfiguration(MockConfiguration.class);
	}

	@AfterEach
	public void tearDown() {
		// Reset static state for clean tests using reflection
		resetTsfContextUtilsStaticFields();
	}

	private void resetTsfContextUtilsStaticFields() {
		try {
			// Reset isOnlyTsfConsulEnabledFirstConfiguration
			Field isOnlyTsfConsulEnabledFirstConfigurationField = TsfContextUtils.class.getDeclaredField("isOnlyTsfConsulEnabledFirstConfiguration");
			isOnlyTsfConsulEnabledFirstConfigurationField.setAccessible(true);
			AtomicBoolean isOnlyTsfConsulEnabledFirstConfiguration = (AtomicBoolean) isOnlyTsfConsulEnabledFirstConfigurationField.get(null);
			isOnlyTsfConsulEnabledFirstConfiguration.set(true);

			// Reset isTsfConsulEnabledFirstConfiguration
			Field isTsfConsulEnabledFirstConfigurationField = TsfContextUtils.class.getDeclaredField("isTsfConsulEnabledFirstConfiguration");
			isTsfConsulEnabledFirstConfigurationField.setAccessible(true);
			AtomicBoolean isTsfConsulEnabledFirstConfiguration = (AtomicBoolean) isTsfConsulEnabledFirstConfigurationField.get(null);
			isTsfConsulEnabledFirstConfiguration.set(true);

			// Reset tsfConsulEnabled
			Field tsfConsulEnabledField = TsfContextUtils.class.getDeclaredField("tsfConsulEnabled");
			tsfConsulEnabledField.setAccessible(true);
			tsfConsulEnabledField.setBoolean(null, false);

			// Reset onlyTsfConsulEnabled
			Field onlyTsfConsulEnabledField = TsfContextUtils.class.getDeclaredField("onlyTsfConsulEnabled");
			onlyTsfConsulEnabledField.setAccessible(true);
			onlyTsfConsulEnabledField.setBoolean(null, false);

		}
		catch (Exception e) {
			throw new RuntimeException("Failed to reset TsfContextUtils static fields", e);
		}
	}

	@Test
	public void testKafkaLaneAspectBeanCreationWhenKafkaTemplatePresent() {
		resetTsfContextUtilsStaticFields();
		// Simulate KafkaTemplate class presence
		contextRunner
				.withPropertyValues("spring.cloud.polaris.lane.kafka.lane-on=true")
				.run(context -> {
					// KafkaLaneAspect should be created when KafkaTemplate is present
					assertThat(context).hasSingleBean(KafkaLaneAspect.class);

					KafkaLaneAspect aspect = context.getBean(KafkaLaneAspect.class);
					assertThat(aspect).isNotNull();
				});
	}

	@Test
	public void testTsfActiveLaneBeanNotCreatedWhenPolarisAddressPresent() {
		resetTsfContextUtilsStaticFields();
		// Simulate Polaris address present (not only TSF Consul)
		contextRunner
				.withPropertyValues(
						"tsf_consul_enable=true",
						"tsf_consul_ip=127.0.0.1",
						"spring.cloud.polaris.address=127.0.0.1:8091"
				)
				.run(context -> {
					// TsfActiveLane should NOT be created when Polaris address is present
					assertThat(context).doesNotHaveBean(TsfActiveLane.class);
				});
	}

	@Test
	public void testKafkaLanePropertiesEnabled() {
		resetTsfContextUtilsStaticFields();
		contextRunner
				.withPropertyValues(
						"spring.cloud.polaris.lane.kafka.lane-on=true",
						"spring.cloud.polaris.lane.kafka.lane-consume-main=true",
						"spring.cloud.polaris.lane.kafka.main-consume-lane=true"
				)
				.run(context -> {
					KafkaLaneProperties properties = context.getBean(KafkaLaneProperties.class);
					assertThat(properties.getLaneOn()).isTrue();
					assertThat(properties.getLaneConsumeMain()).isTrue();
					assertThat(properties.getMainConsumeLane()).isTrue();
				});
	}

	@Test
	public void testBeanDependenciesInjection() {
		resetTsfContextUtilsStaticFields();
		contextRunner
				.withPropertyValues("spring.cloud.polaris.lane.kafka.lane-on=true")
				.run(context -> {
					// Verify that all required dependencies are properly injected
					assertThat(context).hasSingleBean(KafkaLaneAspect.class);

					KafkaLaneAspect aspect = context.getBean(KafkaLaneAspect.class);

					// The aspect should have all required dependencies
					assertThat(aspect).isNotNull();

					// Verify that KafkaLaneProperties is properly configured
					KafkaLaneProperties properties = context.getBean(KafkaLaneProperties.class);
					assertThat(properties).isNotNull();
					assertThat(properties.getLaneOn()).isTrue();
				});
	}

	@Test
	public void testTsfActiveLaneBeanCreationWhenOnlyTsfConsulEnabled() {
		resetTsfContextUtilsStaticFields();
		// Simulate Only TSF Consul enabled condition
		contextRunner
				.withPropertyValues(
						"tsf_consul_enable=true",
						"tsf_consul_ip=127.0.0.1",
						"spring.cloud.polaris.address=" // Empty to simulate only TSF Consul
				)
				.run(context -> {
					// TsfActiveLane should be created when only TSF Consul is enabled
					assertThat(context).hasSingleBean(TsfActiveLane.class);

					TsfActiveLane tsfActiveLane = context.getBean(TsfActiveLane.class);
					assertThat(tsfActiveLane).isNotNull();
				});
	}

	@Configuration
	static class MockConfiguration {
		@Bean
		public PolarisSDKContextManager polarisSDKContextManager() {
			return mock(PolarisSDKContextManager.class);
		}

		@Bean
		public PolarisDiscoveryHandler polarisDiscoveryHandler() {
			return mock(PolarisDiscoveryHandler.class);
		}
	}
}
