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

package com.tencent.cloud.plugin.trafficmirroring.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TrafficMirroringProperties}.
 *
 * @author Haotian Zhang
 */
public class TrafficMirroringPropertiesTest {
	@Test
	void testDefaultValues() {
		TrafficMirroringProperties properties = new TrafficMirroringProperties();

		assertThat(properties.isEnabled()).isFalse();
		assertThat(properties.getRequestPoolSize()).isEqualTo(100);
		assertThat(properties.getRequestConnectionTimeout()).isEqualTo(5000L);
	}

	@Test
	void testEnabledProperty() {
		TrafficMirroringProperties properties = new TrafficMirroringProperties();

		properties.setEnabled(true);

		assertThat(properties.isEnabled()).isTrue();
	}

	@Test
	void testRequestPoolSizeProperty() {
		TrafficMirroringProperties properties = new TrafficMirroringProperties();

		properties.setRequestPoolSize(10);

		assertThat(properties.getRequestPoolSize()).isEqualTo(10);
	}

	@Test
	void testRequestPoolSizeBoundaryValues() {
		TrafficMirroringProperties properties = new TrafficMirroringProperties();

		properties.setRequestPoolSize(0);
		assertThat(properties.getRequestPoolSize()).isZero();

		properties.setRequestPoolSize(-5);
		assertThat(properties.getRequestPoolSize()).isNegative();
	}

	@Test
	void testRequestConnectionTimeoutProperty() {
		TrafficMirroringProperties properties = new TrafficMirroringProperties();

		properties.setRequestConnectionTimeout(3000L);

		assertThat(properties.getRequestConnectionTimeout()).isEqualTo(3000L);
	}

	@Test
	void testRequestConnectionTimeoutBoundaryValues() {
		TrafficMirroringProperties properties = new TrafficMirroringProperties();

		properties.setRequestConnectionTimeout(0L);
		assertThat(properties.getRequestConnectionTimeout()).isZero();

		properties.setRequestConnectionTimeout(-1000L);
		assertThat(properties.getRequestConnectionTimeout()).isNegative();
	}

	@Test
	void testToString() {
		TrafficMirroringProperties properties = new TrafficMirroringProperties();
		properties.setEnabled(true);
		properties.setRequestPoolSize(8);
		properties.setRequestConnectionTimeout(2000L);

		String result = properties.toString();

		assertThat(result)
				.contains("enabled=true")
				.contains("requestPoolSize=8")
				.contains("requestConnectionTimeout=2000");
	}
}
