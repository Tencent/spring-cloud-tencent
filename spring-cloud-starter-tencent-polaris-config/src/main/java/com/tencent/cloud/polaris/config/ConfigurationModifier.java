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

package com.tencent.cloud.polaris.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.util.AddressUtils;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.config.PolarisCryptoConfigProperties;
import com.tencent.cloud.polaris.context.PolarisConfigurationConfigModifier;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.configuration.ConfigFilterConfigImpl;
import com.tencent.polaris.factory.config.configuration.ConnectorConfigImpl;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.LOCAL_FILE_CONNECTOR_TYPE;


/**
 * Read configuration from spring cloud's configuration file and override polaris.yaml.
 *
 * @author lepdou 2022-03-10
 */
public class ConfigurationModifier implements PolarisConfigurationConfigModifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationModifier.class);

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
		configuration.getGlobal().getAPI().setReportEnable(false);
		configuration.getGlobal().getStatReporter().setEnable(false);

		if (!polarisContextProperties.getEnabled() || !polarisConfigProperties.isEnabled()) {
			return;
		}

		initDataSource(configuration);

		ConfigFilterConfigImpl configFilterConfig = configuration.getConfigFile().getConfigFilterConfig();
		configFilterConfig.setEnable(polarisCryptoConfigProperties.isEnabled());
		if (polarisCryptoConfigProperties.isEnabled()) {
			configFilterConfig.getChain().add("crypto");
			configFilterConfig.getPlugin().put("crypto", Collections.singletonMap("type", "AES"));
		}
	}

	private void initDataSource(ConfigurationImpl configuration) {
		// set connector type
		configuration.getConfigFile().getServerConnector().setConnectorType(polarisConfigProperties.getDataSource());
		if (StringUtils.equalsIgnoreCase(polarisConfigProperties.getDataSource(), LOCAL_FILE_CONNECTOR_TYPE)) {
			String localFileRootPath = polarisConfigProperties.getLocalFileRootPath();
			configuration.getConfigFile().getServerConnector().setPersistDir(localFileRootPath);
			LOGGER.info("[SCT] Run spring cloud tencent config with local data source. localFileRootPath = {}", localFileRootPath);
			return;
		}

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

		// enable close check address for unit tests
		if (polarisConfigProperties.isCheckAddress()) {
			checkAddressAccessible(configAddresses);
		}

		configuration.getConfigFile().getServerConnector().setAddresses(configAddresses);

		if (StringUtils.isNotEmpty(polarisConfigProperties.getToken())) {
			ConnectorConfigImpl connectorConfig = configuration.getConfigFile().getServerConnector();
			connectorConfig.setToken(polarisConfigProperties.getToken());
		}

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
