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

package com.tencent.cloud.polaris.config;

import java.util.ArrayList;
import java.util.List;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.util.AddressUtils;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.PolarisContextProperties;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import org.apache.commons.lang.StringUtils;

import org.springframework.util.CollectionUtils;


/**
 * Read configuration from spring cloud's configuration file and override polaris.yaml.
 *
 * @author lepdou 2022-03-10
 */
public class ConfigurationModifier implements PolarisConfigModifier {

	private final PolarisConfigProperties polarisConfigProperties;

	private final PolarisContextProperties polarisContextProperties;

	public ConfigurationModifier(PolarisConfigProperties polarisConfigProperties,
			PolarisContextProperties polarisContextProperties) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.polarisContextProperties = polarisContextProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		// set connector type
		configuration.getConfigFile().getServerConnector().setConnectorType("polaris");

		// set config server address
		List<String> configAddresses;
		String configAddressesStr = polarisConfigProperties.getAddress();

		if (StringUtils.isNotEmpty(configAddressesStr)) {
			configAddresses = AddressUtils.parseAddressList(polarisConfigProperties.getAddress());
		}
		else {
			configAddresses = resolveConfigAddressFromPolarisAddress(polarisContextProperties.getAddress());
		}

		if (CollectionUtils.isEmpty(configAddresses)) {
			throw new RuntimeException("Config server address is blank. Please check your config in bootstrap.yml"
					+ " with spring.cloud.polaris.address or spring.cloud.polaris.config.address");
		}

		configuration.getConfigFile().getServerConnector().setAddresses(configAddresses);
	}

	@Override
	public int getOrder() {
		return ContextConstant.ModifierOrder.CONFIG_ORDER;
	}

	/**
	 * In most cases, the address of the configuration center is the same as that of Polaris, but the port is different.
	 * Therefore, the address of the configuration center can be deduced directly from the Polaris address.
	 *
	 */
	private List<String> resolveConfigAddressFromPolarisAddress(String polarisAddress) {
		if (StringUtils.isEmpty(polarisAddress)) {
			return null;
		}

		List<String> polarisAddresses = AddressUtils.parseAddressList(polarisAddress);
		List<String> configAddresses = new ArrayList<>(polarisAddresses.size());

		for (String address : polarisAddresses) {
			String ip = StringUtils.substringBeforeLast(address, ":");
			configAddresses.add(ip + ":" + polarisConfigProperties.getPort());
		}

		return configAddresses;
	}
}
