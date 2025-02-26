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
 *
 */

package com.tencent.cloud.polaris.config.adapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PolarisRefreshEntireContextRefresher}.
 *
 * @author Shedfree Wu
 */
public class PolarisRefreshEntireContextRefresherTest {
	@Mock
	private PolarisConfigProperties polarisConfigProperties;

	@Mock
	private SpringValueRegistry springValueRegistry;

	@Mock
	private ConfigFileService configFileService;

	@Mock
	private ContextRefresher contextRefresher;

	@Mock
	private ConfigurableApplicationContext applicationContext;

	@Mock
	private PolarisConfigCustomExtensionLayer mockExtensionLayer;

	private PolarisRefreshEntireContextRefresher refresher;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		refresher = new PolarisRefreshEntireContextRefresher(
				polarisConfigProperties,
				springValueRegistry,
				configFileService,
				contextRefresher
		);
		refresher.setApplicationContext(applicationContext);
	}

	@Test
	void testRefreshSpringValue() {
		// test refreshSpringValue method, it should do nothing
		refresher.refreshSpringValue("test.key");

		// Verify
		verifyNoInteractions(contextRefresher);
		verifyNoInteractions(springValueRegistry);
	}

	@Test
	void testRefreshConfigurationPropertiesWithRefreshScope() {
		// Arrange
		Set<String> changeKeys = new HashSet<>();
		changeKeys.add("test.key1");
		changeKeys.add("test.key2");

		// mock test.key1 in refresh scope
		when(springValueRegistry.isRefreshScopeKey("test.key1")).thenReturn(true);

		// Act
		refresher.refreshConfigurationProperties(changeKeys);

		// Verify
		verify(contextRefresher, times(1)).refresh();
		verifyNoInteractions(applicationContext);
	}

	@Test
	void testRefreshConfigurationPropertiesWithoutRefreshScope() {
		// Arrange
		Set<String> changeKeys = new HashSet<>();
		changeKeys.add("test.key1");
		changeKeys.add("test.key2");

		// mock a key not in refresh scope
		when(springValueRegistry.isRefreshScopeKey(anyString())).thenReturn(false);

		// Act
		refresher.refreshConfigurationProperties(changeKeys);

		// Verify
		verify(contextRefresher, never()).refresh();
		verify(applicationContext, times(1))
				.publishEvent(any(EnvironmentChangeEvent.class));
	}

	@Test
	void testSetApplicationContext() {
		// Arrange
		ConfigurableApplicationContext newContext = mock(ConfigurableApplicationContext.class);

		// Act
		refresher.setApplicationContext(newContext);

		// Verify
		Set<String> changeKeys = new HashSet<>();
		changeKeys.add("test.key");
		when(springValueRegistry.isRefreshScopeKey(anyString())).thenReturn(false);

		refresher.refreshConfigurationProperties(changeKeys);
		verify(newContext, times(1)).publishEvent(any(EnvironmentChangeEvent.class));
	}

	@Test
	void testRefreshConfigurationPropertiesWithEmptyChangeKeys() {
		// Arrange
		Set<String> changeKeys = new HashSet<>();

		// Act
		refresher.refreshConfigurationProperties(changeKeys);

		// Verify
		verify(contextRefresher, never()).refresh();
		verify(applicationContext, times(1))
				.publishEvent(any(EnvironmentChangeEvent.class));
	}

	@Test
	void testRefreshConfigurationPropertiesWithMultipleRefreshScopeKeys() {
		// Arrange
		Set<String> changeKeys = new HashSet<>();
		changeKeys.add("test.key1");
		changeKeys.add("test.key2");
		changeKeys.add("test.key3");

		// mock multiple keys in refresh scope
		when(springValueRegistry.isRefreshScopeKey(anyString())).thenReturn(true);

		// Act
		refresher.refreshConfigurationProperties(changeKeys);

		// Verify
		verify(contextRefresher, times(1)).refresh();
		verifyNoInteractions(applicationContext);
	}

	@Test
	void testPolarisConfigCustomExtensionLayer() throws Exception {
		refresher.setRegistered(true);

		Field field = PolarisConfigPropertyAutoRefresher.class
				.getDeclaredField("polarisConfigCustomExtensionLayer");
		field.setAccessible(true);
		field.set(refresher, mockExtensionLayer);

		Method method = PolarisConfigPropertyAutoRefresher.class
				.getDeclaredMethod("customInitRegisterPolarisConfig", PolarisConfigPropertyAutoRefresher.class);
		method.setAccessible(true);
		method.invoke(refresher, refresher);


		method = PolarisConfigPropertyAutoRefresher.class.getDeclaredMethod(
				"customRegisterPolarisConfigPublishChangeListener",
				PolarisPropertySource.class, PolarisPropertySource.class);

		method.setAccessible(true);
		method.invoke(refresher, null, null);

		// Verify
		verify(mockExtensionLayer, times(1)).initRegisterConfig(refresher);

	}

}
