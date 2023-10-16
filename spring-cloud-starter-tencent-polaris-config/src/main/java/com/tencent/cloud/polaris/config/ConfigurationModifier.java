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
import java.util.Collections;
import java.util.List;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.util.AddressUtils;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.config.PolarisCryptoConfigProperties;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.configuration.ConfigFilterConfigImpl;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.CollectionUtils;


/**
 * Read configuration from spring cloud's configuration file and override polaris.yaml.
 *
 * @author lepdou 2022-03-10
 */
public class ConfigurationModifier implements PolarisConfigModifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationModifier.class);

	private static final String DATA_SOURCE_POLARIS = "polaris";
	private static final String DATA_SOURCE_LOCAL = "local";

	private final PolarisConfigProperties polarisConfigProperties;

	private final PolarisCryptoConfigProperties polarisCryptoConfigProperties;

	private final PolarisContextProperties polarisContextProperties;

	public ConfigurationModifier(PolarisConfigProperties polarisConfigProperties,
			PolarisCryptoConfigProperties polarisCryptoConfigProperties,
			PolarisContextProperties polarisContextProperties) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.polarisCryptoConfigProperties = polarisCryptoConfigProperties;
		this.polarisContextProperties = polarisContextProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		if (StringUtils.equalsIgnoreCase(polarisConfigProperties.getDataSource(), DATA_SOURCE_POLARIS)) {
			initByPolarisDataSource(configuration);
		}
		else if (StringUtils.equalsIgnoreCase(polarisConfigProperties.getDataSource(), DATA_SOURCE_LOCAL)) {
			initByLocalDataSource(configuration);
		}
		else {
			throw new RuntimeException("Unsupported config data source");
		}

		ConfigFilterConfigImpl configFilterConfig = configuration.getConfigFile().getConfigFilterConfig();
		configFilterConfig.setEnable(polarisCryptoConfigProperties.isEnabled());
		if (polarisCryptoConfigProperties.isEnabled()) {
			configFilterConfig.getChain().add("crypto");
			configFilterConfig.getPlugin().put("crypto", Collections.singletonMap("type", "AES"));
		}
	}

	private void initByLocalDataSource(ConfigurationImpl configuration) {
		configuration.getConfigFile().getServerConnector().setConnectorType("localFile");

		String localFileRootPath = polarisConfigProperties.getLocalFileRootPath();
		configuration.getConfigFile().getServerConnector().setPersistDir(localFileRootPath);

		LOGGER.info("[SCT] Run spring cloud tencent config with local data source. localFileRootPath = {}", localFileRootPath);
	}

	private void initByPolarisDataSource(ConfigurationImpl configuration) {
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

		checkAddressAccessible(configAddresses);

		configuration.getConfigFile().getServerConnector().setAddresses(configAddresses);

		LOGGER.info("[SCT] Run spring cloud tencent config in polaris data source.");
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.CONFIG_ORDER;
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

	private void checkAddressAccessible(List<String> configAddresses) {
		// check address can connect
		configAddresses.forEach(address -> {
			String[] ipPort = address.split(":");

			if (ipPort.length != 2) {
				throw new IllegalArgumentException("Config server address (" + address + ") is wrong, please check address like grpc://183.47.111.8:8091.");
			}

			if (!AddressUtils.accessible(ipPort[0], Integer.parseInt(ipPort[1]), 3000)) {
				String errMsg = "Config server address (" + address + ") can not be connected. Please check your config in bootstrap.yml"
						+ " with spring.cloud.polaris.address or spring.cloud.polaris.config.address.";
				if (polarisConfigProperties.isShutdownIfConnectToConfigServerFailed()) {
					throw new IllegalArgumentException(errMsg);
				}
				else {
					LOGGER.error(errMsg);
				}
			}
		});
	}
}
