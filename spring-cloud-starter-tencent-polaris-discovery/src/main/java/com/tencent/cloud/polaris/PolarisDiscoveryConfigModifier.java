/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.DiscoveryConfigImpl;
import com.tencent.polaris.factory.config.provider.RegisterConfigImpl;

/**
 * @author lingxiao.wlx
 */
public class PolarisDiscoveryConfigModifier implements PolarisConfigModifier {

	private static final String ID = "polaris";

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	public PolarisDiscoveryConfigModifier(PolarisDiscoveryProperties polarisDiscoveryProperties) {
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		if (polarisDiscoveryProperties != null) {
			DiscoveryConfigImpl discoveryConfig = new DiscoveryConfigImpl();
			discoveryConfig.setServerConnectorId(ID);
			discoveryConfig.setEnable(polarisDiscoveryProperties.isEnabled());
			configuration.getConsumer().getDiscoveries().add(discoveryConfig);

			RegisterConfigImpl registerConfig = new RegisterConfigImpl();
			registerConfig.setServerConnectorId(ID);
			registerConfig.setEnable(polarisDiscoveryProperties.isRegisterEnabled());
			configuration.getProvider().getRegisters().add(registerConfig);
		}
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.DISCOVERY_CONFIG_ORDER;
	}
}
