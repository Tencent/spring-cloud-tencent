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

package com.tencent.cloud.polaris.config;

import java.util.Arrays;
import java.util.List;

import com.tencent.cloud.polaris.context.ModifyAddress;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.context.event.PushGatewayEventReporterConfigModifier;
import com.tencent.cloud.polaris.context.event.PushGatewayEventReporterProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PolarisConfigSDKContextManager}.
 *
 * @author fishtailfu
 */
@DisplayName("PolarisConfigSDKContextManager")
public class PolarisConfigSDKContextManagerTest {

	/**
	 * The config SDK context reports config events, so it takes the push gateway modifier along with
	 * the config modifiers, but not the modifiers of other features.
	 */
	@DisplayName("select config modifiers and push gateway modifier")
	@Test
	public void testSelectConfigModifiers() {
		// Arrange
		ConfigurationModifier configurationModifier = mock(ConfigurationModifier.class);
		PushGatewayEventReporterConfigModifier pushGatewayModifier =
				new PushGatewayEventReporterConfigModifier(new PushGatewayEventReporterProperties());
		ModifyAddress modifyAddress = new ModifyAddress(new PolarisContextProperties());
		List<PolarisConfigModifier> modifierList = Arrays.asList(configurationModifier, pushGatewayModifier,
				modifyAddress);

		// Act
		List<PolarisConfigModifier> result = PolarisConfigSDKContextManager.selectConfigModifiers(modifierList);

		// Assert
		assertThat(result).containsExactly(configurationModifier, pushGatewayModifier);
	}

	/**
	 * Without config modifiers only the push gateway modifier is selected, and the caller creates no
	 * config SDK context.
	 */
	@DisplayName("select only push gateway modifier without config modifiers")
	@Test
	public void testSelectConfigModifiersWithoutConfigurationModifier() {
		// Arrange
		PushGatewayEventReporterConfigModifier pushGatewayModifier =
				new PushGatewayEventReporterConfigModifier(new PushGatewayEventReporterProperties());
		ModifyAddress modifyAddress = new ModifyAddress(new PolarisContextProperties());

		// Act
		List<PolarisConfigModifier> result = PolarisConfigSDKContextManager.selectConfigModifiers(
				Arrays.asList(pushGatewayModifier, modifyAddress));

		// Assert
		assertThat(result).containsExactly(pushGatewayModifier);
	}
}
