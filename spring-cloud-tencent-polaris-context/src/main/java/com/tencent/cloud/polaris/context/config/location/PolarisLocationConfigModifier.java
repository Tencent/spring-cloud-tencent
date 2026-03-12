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

import java.util.List;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.api.plugin.location.LocationProvider;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.LocationConfigImpl;
import com.tencent.polaris.factory.config.global.LocationProviderConfigImpl;

/**
 * Config modifier for Polaris location providers.
 *
 * @author Haotian Zhang
 */
public class PolarisLocationConfigModifier implements PolarisConfigModifier {

	private final PolarisLocationProperties properties;

	public PolarisLocationConfigModifier(PolarisLocationProperties properties) {
		this.properties = properties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		LocationConfigImpl locationConfig = (LocationConfigImpl) configuration.getGlobal().getLocation();
		List<LocationProviderConfigImpl> providers = locationConfig.getProviders();
		if (providers == null) {
			return;
		}

		String cloudTypeName = LocationProvider.ProviderType.CLOUD.getName();

		if (!properties.getCloud().isEnabled()) {
			// Remove cloud provider if disabled
			providers.removeIf(p -> cloudTypeName.equals(p.getType()));
			return;
		}

		// Only add the cloud provider entry if it does not already exist
		boolean cloudProviderExists = providers.stream()
				.anyMatch(p -> cloudTypeName.equals(p.getType()));
		if (!cloudProviderExists) {
			LocationProviderConfigImpl cloudProviderConfig = new LocationProviderConfigImpl();
			cloudProviderConfig.setType(cloudTypeName);
			providers.add(cloudProviderConfig);
		}
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.LOCATION_ORDER;
	}
}
