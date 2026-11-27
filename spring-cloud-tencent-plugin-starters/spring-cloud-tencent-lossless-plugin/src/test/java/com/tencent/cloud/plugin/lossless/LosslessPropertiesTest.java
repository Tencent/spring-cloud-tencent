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

package com.tencent.cloud.plugin.lossless;

import com.tencent.cloud.plugin.lossless.config.LosslessProperties;
import com.tencent.cloud.plugin.lossless.extension.OfflineCallbackFailureMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link LosslessProperties}.
 *
 * @author Shedfree Wu
 */
public class LosslessPropertiesTest {

	@DisplayName("test get and set")
	@Test
	void testGetAndSet() {
		LosslessProperties polarisStatProperties = new LosslessProperties();

		// healthCheckPath
		polarisStatProperties.setHealthCheckPath("/xxx");
		assertThat(polarisStatProperties.getHealthCheckPath()).isEqualTo("/xxx");
	}

	/**
	 * Verify offline callback properties reject invalid values.
	 */
	@DisplayName("test offline callback property validation")
	@Test
	void testOfflineCallbackPropertyValidation() {
		// Arrange
		LosslessProperties properties = new LosslessProperties();

		// Act and assert
		assertThatThrownBy(() -> properties.setOfflineCallbackTimeout(0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> properties.setOfflineCallbackTotalTimeout(0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> properties.setOfflineCallbackFailureMode(null))
				.isInstanceOf(IllegalArgumentException.class);
		properties.setOfflineCallbackFailureMode(OfflineCallbackFailureMode.ABORT);
		assertThat(properties.isOfflineCallbackEnabled()).isTrue();
		assertThat(properties.getOfflineCallbackTimeout()).isEqualTo(10000);
		assertThat(properties.getOfflineCallbackTotalTimeout()).isEqualTo(20000);
		assertThat(properties.getOfflineCallbackFailureMode()).isEqualTo(OfflineCallbackFailureMode.ABORT);
	}

	/**
	 * Verify the total timeout accepts the supported maximum and rejects overflow-prone values.
	 */
	@DisplayName("test offline callback total timeout upper bound")
	@Test
	void testOfflineCallbackTotalTimeoutUpperBound() {
		// Arrange
		LosslessProperties properties = new LosslessProperties();
		long maximum = Long.MAX_VALUE / 1_000_000L;

		// Act and assert
		properties.setOfflineCallbackTotalTimeout(maximum);
		assertThat(properties.getOfflineCallbackTotalTimeout()).isEqualTo(maximum);
		assertThatThrownBy(() -> properties.setOfflineCallbackTotalTimeout(maximum + 1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("offlineCallbackTotalTimeout is too large");
	}
}
