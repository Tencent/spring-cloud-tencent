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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisLocationProperties}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisLocationProperties")
class PolarisLocationPropertiesTest {

	/**
	 * Test purpose: verify PolarisLocationProperties default values.
	 * Test scenario: no configuration provided, use default constructor.
	 * Verification: cloud.enabled is true by default.
	 */
	@DisplayName("cloud.enabled defaults to true")
	@Test
	void testDefaultCloudEnabled() {
		// Arrange
		PolarisLocationProperties properties = new PolarisLocationProperties();

		// Act & Assert
		assertThat(properties.getCloud().isEnabled()).isTrue();
	}

	/**
	 * Test purpose: verify PolarisLocationProperties.Cloud setter and getter.
	 * Test scenario: set cloud.enabled to false via setter.
	 * Verification: getter returns false.
	 */
	@DisplayName("Cloud enabled can be set to false")
	@Test
	void testSetCloudEnabled() {
		// Arrange
		PolarisLocationProperties properties = new PolarisLocationProperties();
		PolarisLocationProperties.Cloud cloud = new PolarisLocationProperties.Cloud();
		cloud.setEnabled(false);

		// Act
		properties.setCloud(cloud);

		// Assert
		assertThat(properties.getCloud().isEnabled()).isFalse();
	}

	/**
	 * Test purpose: verify PolarisLocationProperties toString output.
	 * Test scenario: default properties instance.
	 * Verification: toString contains cloud and enabled info.
	 */
	@DisplayName("toString contains cloud info")
	@Test
	void testToString() {
		// Arrange
		PolarisLocationProperties properties = new PolarisLocationProperties();

		// Act
		String result = properties.toString();

		// Assert
		assertThat(result).contains("cloud=");
		assertThat(result).contains("enabled=true");
	}

	/**
	 * Test purpose: verify PolarisLocationProperties.Cloud toString output.
	 * Test scenario: default Cloud instance.
	 * Verification: toString contains enabled=true.
	 */
	@DisplayName("Cloud toString contains enabled info")
	@Test
	void testCloudToString() {
		// Arrange
		PolarisLocationProperties.Cloud cloud = new PolarisLocationProperties.Cloud();

		// Act
		String result = cloud.toString();

		// Assert
		assertThat(result).contains("enabled=true");
	}
}
