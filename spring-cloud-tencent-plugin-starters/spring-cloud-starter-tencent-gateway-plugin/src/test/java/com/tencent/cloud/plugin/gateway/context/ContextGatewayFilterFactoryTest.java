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

package com.tencent.cloud.plugin.gateway.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.gateway.filter.GatewayFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextGatewayFilterFactory}.
 */
class ContextGatewayFilterFactoryTest {

	@Mock
	private ContextGatewayPropertiesManager mockManager;

	private ContextGatewayFilterFactory filterFactory;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		filterFactory = new ContextGatewayFilterFactory(mockManager);
	}

	// Test filter factory creates filter with correct configuration
	@Test
	void shouldCreateFilterWithValidConfig() {
		// Setup test config
		ContextGatewayFilterFactory.Config config = new ContextGatewayFilterFactory.Config();
		config.setGroup("test-group");

		// Create filter instance
		GatewayFilter filter = filterFactory.apply(config);

		// Verify filter creation
		assertThat(filter)
				.isInstanceOf(ContextGatewayFilter.class)
				.isNotNull();
	}

	// Test shortcut field order configuration
	@Test
	void shouldReturnCorrectShortcutFieldOrder() {
		// Execute & Verify
		assertThat(filterFactory.shortcutFieldOrder())
				.containsExactly("group");
	}

	// Test config class getter/setter behavior
	@Test
	void shouldHandleConfigGroupPropertyCorrectly() {
		// Setup config
		ContextGatewayFilterFactory.Config config = new ContextGatewayFilterFactory.Config();
		final String expectedGroup = "payment-service";

		// Test setter/getter
		config.setGroup(expectedGroup);
		assertThat(config.getGroup())
				.isEqualTo(expectedGroup);
	}


	// Test filter creation with empty group name
	@Test
	void shouldHandleEmptyGroupNameInConfig() {
		// Setup config with empty group
		ContextGatewayFilterFactory.Config config = new ContextGatewayFilterFactory.Config();
		config.setGroup("");

		// Create filter instance
		GatewayFilter filter = filterFactory.apply(config);

		// Verify filter creation
		assertThat(filter)
				.isNotNull()
				.isInstanceOf(ContextGatewayFilter.class);
	}
}
