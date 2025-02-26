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

package com.tencent.cloud.rpc.enhancement.plugin;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.serviceregistry.Registration;

/**
 * Tests for {@link DefaultEnhancedPluginRunner}.
 *
 * @author Shedfree Wu
 */
@ExtendWith(MockitoExtension.class)
public class DefaultEnhancedPluginRunnerTest {

	@Test
	void getPolarisRegistration_WithPolarisRegistration_ReturnsPolarisRegistration() {
		// Arrange
		List<Registration> registrations = new ArrayList<>();
		Registration normalReg = createMockRegistration();
		Registration normalReg2 = createMockRegistration();
		registrations.add(normalReg);
		registrations.add(normalReg2);

		// Act
		Registration result = DefaultEnhancedPluginRunner.getPolarisRegistration(registrations);

		// Assert
		Assertions.assertSame(normalReg, result);
	}

	// Helper method to create mock Registration objects
	private Registration createMockRegistration() {
		return new Registration() {
			@Override
			public String getServiceId() {
				return null;
			}

			@Override
			public String getHost() {
				return null;
			}

			@Override
			public int getPort() {
				return 0;
			}

			@Override
			public boolean isSecure() {
				return false;
			}

			@Override
			public java.net.URI getUri() {
				return null;
			}

			@Override
			public java.util.Map<String, String> getMetadata() {
				return null;
			}
		};
	}

}
