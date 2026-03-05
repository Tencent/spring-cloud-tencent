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

package com.tencent.cloud.polaris.context.config.extend.tsf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.polaris.plugins.router.lane.BaseLaneMode;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link TsfCoreEnvironmentPostProcessor}.
 *
 * @author Haotian Zhang
 */
@DisplayName("TsfCoreEnvironmentPostProcessor")
class TsfCoreEnvironmentPostProcessorTest {

	private TsfCoreEnvironmentPostProcessor processor;

	private SpringApplication application;

	@BeforeEach
	void setUp() throws Exception {
		// Create processor via reflection since constructor is private
		Constructor<TsfCoreEnvironmentPostProcessor> constructor =
				TsfCoreEnvironmentPostProcessor.class.getDeclaredConstructor(DeferredLogFactory.class);
		constructor.setAccessible(true);
		DeferredLogFactory logFactory = mock(DeferredLogFactory.class);
		Log mockLog = mock(Log.class);
		Mockito.when(logFactory.getLog(Mockito.any(Class.class))).thenReturn(mockLog);
		processor = constructor.newInstance(logFactory);
		application = mock(SpringApplication.class);

		// Reset TsfContextUtils static state before each test
		resetTsfContextUtilsState();
	}

	/**
	 * Test that getOrder returns the correct order value.
	 * Scenario: verify the processor runs before ConfigDataEnvironmentPostProcessor.
	 * Verifies: order is ConfigDataEnvironmentPostProcessor.ORDER - 1.
	 */
	@DisplayName("Test getOrder returns correct value")
	@Test
	void testGetOrder() {
		// Assert
		assertThat(processor.getOrder()).isEqualTo(ConfigDataEnvironmentPostProcessor.ORDER - 1);
	}

	/**
	 * Test that no properties are added when tsf_app_id is not set.
	 * Scenario: non-TSF deployment environment.
	 * Verifies: no "tsf-polaris-properties" property source is added.
	 */
	@DisplayName("Test postProcessEnvironment when tsf_app_id is not set")
	@Test
	void testPostProcessEnvironment_NonTsfDeploy() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();

		// Act
		processor.postProcessEnvironment(environment, application);

		// Assert
		PropertySource<?> propertySource = environment.getPropertySources().get("tsf-polaris-properties");
		assertThat(propertySource).isNull();
	}

	/**
	 * Test that polaris enabled property is set when tsf_app_id is present.
	 * Scenario: TSF deployment without consul, only tsf_app_id is set.
	 * Verifies: spring.cloud.polaris.enabled is set to "true".
	 */
	@DisplayName("Test postProcessEnvironment with tsf_app_id only")
	@Test
	void testPostProcessEnvironment_TsfAppIdOnly() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("tsf_app_id", "app-123");

		// Act
		processor.postProcessEnvironment(environment, application);

		// Assert
		assertThat(environment.getProperty("spring.cloud.polaris.enabled")).isEqualTo("true");
	}

	/**
	 * Test that polaris enabled is not overridden when already set.
	 * Scenario: TSF deployment where spring.cloud.polaris.enabled is already configured.
	 * Verifies: the existing value is preserved.
	 */
	@DisplayName("Test postProcessEnvironment does not override existing polaris enabled")
	@Test
	void testPostProcessEnvironment_ExistingPolarisEnabled() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("tsf_app_id", "app-123");
		environment.setProperty("spring.cloud.polaris.enabled", "false");

		// Act
		processor.postProcessEnvironment(environment, application);

		// Assert
		// tsf-polaris-properties is added first, but original property source should take effect
		// since the existing value was set, the defaultProperties should not contain this key
		PropertySource<?> propertySource = environment.getPropertySources().get("tsf-polaris-properties");
		assertThat(propertySource).isNotNull();
		// The tsf-polaris-properties is added first, so it overrides. But since polaris.enabled was already set,
		// it should not be added to defaultProperties
		assertThat(propertySource.getProperty("spring.cloud.polaris.enabled")).isNull();
	}

	/**
	 * Test lossless and stat properties are set when polaris_admin_port is present.
	 * Scenario: TSF deployment with admin port configured.
	 * Verifies: lossless and stat properties are enabled.
	 */
	@DisplayName("Test postProcessEnvironment with polaris_admin_port")
	@Test
	void testPostProcessEnvironment_WithAdminPort() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("tsf_app_id", "app-123");
		environment.setProperty("polaris_admin_port", "8080");

		// Act
		processor.postProcessEnvironment(environment, application);

		// Assert
		assertThat(environment.getProperty("spring.cloud.polaris.lossless.enabled")).isEqualTo("true");
		assertThat(environment.getProperty("spring.cloud.polaris.stat.enabled")).isEqualTo("true");
	}

	/**
	 * Test IPv6 preference property is set when tsf_prefer_ipv6 is present.
	 * Scenario: TSF deployment with IPv6 preference configured.
	 * Verifies: discovery prefer-ipv6 property is set.
	 */
	@DisplayName("Test postProcessEnvironment with IPv6 preference")
	@Test
	void testPostProcessEnvironment_WithIpv6Preference() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("tsf_app_id", "app-123");
		environment.setProperty("tsf_prefer_ipv6", "true");

		// Act
		processor.postProcessEnvironment(environment, application);

		// Assert
		assertThat(environment.getProperty("spring.cloud.polaris.discovery.prefer-ipv6")).isEqualTo("true");
	}

	/**
	 * Test consul-related properties are set when TSF consul is enabled.
	 * Scenario: TSF deployment with consul enabled (consul ip/port/token set, and polaris_address also set).
	 * Verifies: consul connection properties, discovery properties, and contract properties are set.
	 */
	@DisplayName("Test postProcessEnvironment with TSF consul enabled (not only consul)")
	@Test
	void testPostProcessEnvironment_TsfConsulEnabled() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("tsf_app_id", "app-123");
		environment.setProperty("tsf_consul_enable", "true");
		environment.setProperty("tsf_consul_ip", "127.0.0.1");
		environment.setProperty("tsf_consul_port", "8500");
		environment.setProperty("tsf_token", "test-token");
		environment.setProperty("tsf_instance_id", "instance-001");
		environment.setProperty("tsf_application_id", "application-001");
		environment.setProperty("tsf_group_id", "group-001");
		environment.setProperty("tsf_namespace_id", "namespace-001");
		environment.setProperty("polaris_address", "grpc://polaris.example.com:8091");

		// Act
		processor.postProcessEnvironment(environment, application);

		// Assert
		assertThat(environment.getProperty("spring.cloud.polaris.enabled")).isEqualTo("true");
		assertThat(environment.getProperty("spring.cloud.consul.enabled")).isEqualTo("true");
		assertThat(environment.getProperty("spring.cloud.consul.host")).isEqualTo("127.0.0.1");
		assertThat(environment.getProperty("spring.cloud.consul.port")).isEqualTo("8500");
		assertThat(environment.getProperty("spring.cloud.consul.token")).isEqualTo("test-token");
		assertThat(environment.getProperty("spring.cloud.consul.discovery.instance-id")).isEqualTo("instance-001");
		assertThat(environment.getProperty("spring.cloud.polaris.discovery.instance-id")).isEqualTo("instance-001");
		assertThat(environment.getProperty("spring.cloud.polaris.namespace")).isEqualTo("namespace-001");

		// discovery - zero protection and warmup
		assertThat(environment.getProperty("spring.cloud.polaris.discovery.zero-protection.enabled")).isEqualTo("true");
		assertThat(environment.getProperty("spring.cloud.polaris.discovery.zero-protection.is-need-test-connectivity")).isEqualTo("true");
		assertThat(environment.getProperty("spring.cloud.discovery.client.health-indicator.enabled")).isEqualTo("false");
		assertThat(environment.getProperty("spring.cloud.polaris.warmup.enabled")).isEqualTo("true");

		// contract
		assertThat(environment.getProperty("spring.cloud.polaris.contract.enabled")).isEqualTo("true");
		assertThat(environment.getProperty("spring.cloud.polaris.contract.name")).isEqualTo("application-001");
		assertThat(environment.getProperty("spring.cloud.polaris.contract.exposure")).isEqualTo("true");
		assertThat(environment.getProperty("spring.cloud.polaris.contract.report.enabled")).isEqualTo("true");

		// should NOT have only-consul properties
		assertThat(environment.getProperty("spring.cloud.polaris.discovery.enabled")).isNull();
		assertThat(environment.getProperty("spring.cloud.polaris.config.data-source")).isNull();
	}

	/**
	 * Test properties for only-consul mode (no polaris_address set).
	 * Scenario: TSF deployment with only consul (no polaris server address).
	 * Verifies: polaris discovery is disabled, config data source is consul, router properties are set.
	 */
	@DisplayName("Test postProcessEnvironment with only TSF consul enabled")
	@Test
	void testPostProcessEnvironment_OnlyTsfConsulEnabled() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("tsf_app_id", "app-123");
		environment.setProperty("tsf_consul_enable", "true");
		environment.setProperty("tsf_consul_ip", "127.0.0.1");
		environment.setProperty("tsf_consul_port", "8500");
		environment.setProperty("tsf_token", "test-token");
		environment.setProperty("tsf_instance_id", "instance-001");
		environment.setProperty("tsf_application_id", "application-001");
		environment.setProperty("tsf_group_id", "group-001");
		environment.setProperty("tsf_namespace_id", "namespace-001");
		// No polaris_address set => only consul mode

		// Act
		processor.postProcessEnvironment(environment, application);

		// Assert - only consul mode specific properties
		assertThat(environment.getProperty("spring.cloud.polaris.discovery.enabled")).isEqualTo("false");
		assertThat(environment.getProperty("spring.cloud.polaris.discovery.register")).isEqualTo("false");

		// configuration
		assertThat(environment.getProperty("spring.cloud.polaris.config.enabled")).isEqualTo("true");
		assertThat(environment.getProperty("spring.cloud.polaris.config.internal-enabled")).isEqualTo("false");
		assertThat(environment.getProperty("spring.cloud.polaris.config.check-address")).isEqualTo("false");
		assertThat(environment.getProperty("spring.cloud.polaris.config.data-source")).isEqualTo("consul");
		assertThat(environment.getProperty("spring.cloud.polaris.config.address")).isEqualTo("http://127.0.0.1:8500");
		assertThat(environment.getProperty("spring.cloud.polaris.config.port")).isEqualTo("8500");
		assertThat(environment.getProperty("spring.cloud.polaris.config.token")).isEqualTo("test-token");
		assertThat(environment.getProperty("spring.cloud.polaris.config.groups[0].namespace")).isEqualTo("config");
		assertThat(environment.getProperty("spring.cloud.polaris.config.groups[0].name")).isEqualTo("application");
		assertThat(environment.getProperty("spring.cloud.polaris.config.groups[0].files[0]")).isEqualTo("application-001/group-001/");
		assertThat(environment.getProperty("spring.cloud.polaris.config.groups[0].files[1]")).isEqualTo("namespace-001/");
		assertThat(environment.getProperty("spring.cloud.polaris.config.refresh-type")).isEqualTo("refresh_context");

		// router
		assertThat(environment.getProperty("spring.cloud.polaris.router.rule-router.fail-over")).isEqualTo("none");
		assertThat(environment.getProperty("spring.cloud.polaris.router.namespace-router.enabled")).isEqualTo("true");
		assertThat(environment.getProperty("spring.cloud.polaris.discovery.all-recover-enabled")).isEqualTo("false");
		assertThat(environment.getProperty("spring.cloud.polaris.router.lane-router.enabled")).isEqualTo("true");
		assertThat(environment.getProperty("spring.cloud.polaris.router.lane-router.baseLaneMode"))
				.isEqualTo(BaseLaneMode.EXCLUDE_ENABLED_LANE_INSTANCE.name());
	}

	/**
	 * Test that polaris namespace is not overridden when polaris_namespace is already set.
	 * Scenario: TSF consul enabled with both tsf_namespace_id and polaris_namespace set.
	 * Verifies: polaris_namespace takes precedence.
	 */
	@DisplayName("Test postProcessEnvironment does not override polaris_namespace")
	@Test
	void testPostProcessEnvironment_ExistingPolarisNamespace() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("tsf_app_id", "app-123");
		environment.setProperty("tsf_consul_enable", "true");
		environment.setProperty("tsf_consul_ip", "127.0.0.1");
		environment.setProperty("tsf_consul_port", "8500");
		environment.setProperty("tsf_token", "test-token");
		environment.setProperty("tsf_instance_id", "instance-001");
		environment.setProperty("tsf_application_id", "application-001");
		environment.setProperty("tsf_group_id", "group-001");
		environment.setProperty("tsf_namespace_id", "namespace-001");
		environment.setProperty("polaris_namespace", "custom-namespace");
		environment.setProperty("polaris_address", "grpc://polaris.example.com:8091");

		// Act
		processor.postProcessEnvironment(environment, application);

		// Assert
		PropertySource<?> propertySource = environment.getPropertySources().get("tsf-polaris-properties");
		assertThat(propertySource).isNotNull();
		// polaris_namespace is already set, so spring.cloud.polaris.namespace should not be in defaultProperties
		assertThat(propertySource.getProperty("spring.cloud.polaris.namespace")).isNull();
	}

	/**
	 * Test contract properties with custom swagger configuration.
	 * Scenario: TSF consul enabled with custom swagger base package and exclude path.
	 * Verifies: contract base-package and exclude-path are set from swagger config.
	 */
	@DisplayName("Test postProcessEnvironment with custom swagger config")
	@Test
	void testPostProcessEnvironment_CustomSwaggerConfig() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("tsf_app_id", "app-123");
		environment.setProperty("tsf_consul_enable", "true");
		environment.setProperty("tsf_consul_ip", "127.0.0.1");
		environment.setProperty("tsf_consul_port", "8500");
		environment.setProperty("tsf_token", "test-token");
		environment.setProperty("tsf_instance_id", "instance-001");
		environment.setProperty("tsf_application_id", "application-001");
		environment.setProperty("tsf_group_id", "group-001");
		environment.setProperty("tsf_namespace_id", "namespace-001");
		environment.setProperty("polaris_address", "grpc://polaris.example.com:8091");
		environment.setProperty("tsf.swagger.basePackage", "com.tencent.cloud.demo");
		environment.setProperty("tsf.swagger.excludePath", "/health,/info");
		environment.setProperty("tsf.swagger.group", "custom-group");
		environment.setProperty("tsf.swagger.basePath", "/api/**");

		// Act
		processor.postProcessEnvironment(environment, application);

		// Assert
		assertThat(environment.getProperty("spring.cloud.polaris.contract.base-package")).isEqualTo("com.tencent.cloud.demo");
		assertThat(environment.getProperty("spring.cloud.polaris.contract.exclude-path")).isEqualTo("/health,/info");
		assertThat(environment.getProperty("spring.cloud.polaris.contract.group")).isEqualTo("custom-group");
		assertThat(environment.getProperty("spring.cloud.polaris.contract.base-path")).isEqualTo("/api/**");
	}

	/**
	 * Test that tsf-polaris-properties is added first in property sources.
	 * Scenario: TSF deployment with tsf_app_id set.
	 * Verifies: the property source is added with addFirst.
	 */
	@DisplayName("Test property source is added first")
	@Test
	void testPostProcessEnvironment_PropertySourceAddedFirst() {
		// Arrange
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("tsf_app_id", "app-123");

		// Act
		processor.postProcessEnvironment(environment, application);

		// Assert
		PropertySource<?> firstSource = environment.getPropertySources().stream().findFirst().orElse(null);
		assertThat(firstSource).isNotNull();
		assertThat(firstSource.getName()).isEqualTo("tsf-polaris-properties");
	}

	/**
	 * Reset the static AtomicBoolean fields in TsfContextUtils to allow re-evaluation in each test.
	 */
	private void resetTsfContextUtilsState() throws Exception {
		Field isTsfConsulEnabledFirst = TsfContextUtils.class.getDeclaredField("isTsfConsulEnabledFirstConfiguration");
		isTsfConsulEnabledFirst.setAccessible(true);
		((AtomicBoolean) isTsfConsulEnabledFirst.get(null)).set(true);

		Field isOnlyTsfConsulEnabledFirst = TsfContextUtils.class.getDeclaredField("isOnlyTsfConsulEnabledFirstConfiguration");
		isOnlyTsfConsulEnabledFirst.setAccessible(true);
		((AtomicBoolean) isOnlyTsfConsulEnabledFirst.get(null)).set(true);

		Field tsfConsulEnabledField = TsfContextUtils.class.getDeclaredField("tsfConsulEnabled");
		tsfConsulEnabledField.setAccessible(true);
		tsfConsulEnabledField.set(null, false);

		Field onlyTsfConsulEnabledField = TsfContextUtils.class.getDeclaredField("onlyTsfConsulEnabled");
		onlyTsfConsulEnabledField.setAccessible(true);
		onlyTsfConsulEnabledField.set(null, false);
	}
}
