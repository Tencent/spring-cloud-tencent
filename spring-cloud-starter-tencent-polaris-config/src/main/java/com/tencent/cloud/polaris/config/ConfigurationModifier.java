/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 *  Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/BSD-3-Clause
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package com.tencent.cloud.polaris.config;

import java.util.List;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.util.AddressUtils;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * Read configuration from spring cloud's configuration file and override polaris.yaml.
 *
 * @author lepdou 2022-03-10
 */
public class ConfigurationModifier implements PolarisConfigModifier {

	@Autowired
	private PolarisConfigProperties polarisConfigProperties;

	@Override
	public void modify(ConfigurationImpl configuration) {
		configuration.getConfigFile().getServerConnector().setConnectorType("polaris");

		if (StringUtils.isEmpty(polarisConfigProperties.getAddresses())) {
			return;
		}

		// override polaris config server address
		List<String> addresses = AddressUtils
				.parseAddressList(polarisConfigProperties.getAddresses());

		configuration.getConfigFile().getServerConnector().setAddresses(addresses);
	}

	@Override
	public int getOrder() {
		return ContextConstant.ModifierOrder.CONFIG_ORDER;
	}

}
