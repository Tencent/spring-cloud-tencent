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

package com.tencent.cloud.plugin.gateway;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.polaris.registry.PolarisRegistration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayRegistrationCustomizerTest {

	@Mock
	private PolarisRegistration polarisRegistration;

	private final GatewayRegistrationCustomizer customizer = new GatewayRegistrationCustomizer();

	@Test
	void shouldAddServiceTypeMetadata() {
		// Arrange
		Map<String, String> metadata = new HashMap<>();
		when(polarisRegistration.getMetadata()).thenReturn(metadata);

		// Act
		customizer.customize(polarisRegistration);

		// Assert
		assertThat(metadata)
				.containsEntry("internal-service-type", "spring-cloud-gateway");
	}

	@Test
	void shouldNotOverrideExistingMetadata() {
		// Arrange
		Map<String, String> metadata = new HashMap<>();
		metadata.put("existing-key", "existing-value");
		metadata.put("internal-service-type", "existing-type");
		when(polarisRegistration.getMetadata()).thenReturn(metadata);

		// Act
		customizer.customize(polarisRegistration);

		// Assert
		assertThat(metadata)
				.containsEntry("internal-service-type", "spring-cloud-gateway")
				.containsEntry("existing-key", "existing-value");
	}
}
