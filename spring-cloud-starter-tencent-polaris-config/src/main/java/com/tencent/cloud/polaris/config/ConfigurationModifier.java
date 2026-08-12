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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.cloud.common.util.AddressUtils;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.config.PolarisCryptoConfigProperties;
import com.tencent.cloud.polaris.context.PolarisConfigurationConfigModifier;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.configuration.client.internal.ConfigWatchReportRequestCustomizer;
import com.tencent.polaris.configuration.client.internal.ConfigWatchReportRequestCustomizerConfig;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.configuration.ConfigFilterConfigImpl;
import com.tencent.polaris.factory.config.configuration.ConnectorConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
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
		configuration.getGlobal().getStatReporter().setEnable(false);
		PluginConfigImpl customizerConfig = configuration.getGlobal()
				.getReportClientRequestCustomizer();
		ConfigWatchReportRequestCustomizerConfig configWatchCustomizerConfig = customizerConfig.getPluginConfig(
				ConfigWatchReportRequestCustomizer.NAME, ConfigWatchReportRequestCustomizerConfig.class);
		configWatchCustomizerConfig.setEnable(polarisConfigProperties.getReport().isEnabled());
		customizerConfig.setPluginConfig(ConfigWatchReportRequestCustomizer.NAME, configWatchCustomizerConfig);
		configuration.getConsumer().getOutlierDetection().setWhen(OutlierDetectionConfig.When.never);
		configuration.getConsumer().getCircuitBreaker().setEnable(false);

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
		ConnectorConfigImpl connectorConfig = configuration.getConfigFile().getServerConnector();
		// set connector type
		connectorConfig.setConnectorType(polarisConfigProperties.getDataSource());
		if (StringUtils.equalsIgnoreCase(polarisConfigProperties.getDataSource(), LOCAL_FILE_CONNECTOR_TYPE)) {
			String localFileRootPath = polarisConfigProperties.getLocalFileRootPath();
			connectorConfig.setPersistDir(localFileRootPath);
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

		connectorConfig.setAddresses(configAddresses);
		connectorConfig.setLbPolicy(polarisContextProperties.getAddressLbPolicy());
		connectorConfig.setServerSwitchInterval(polarisContextProperties.getServerSwitchInterval());

		if (StringUtils.isNotEmpty(polarisConfigProperties.getToken())) {
			connectorConfig.setToken(polarisConfigProperties.getToken());
		}

		connectorConfig.setEmptyProtectionEnable(polarisConfigProperties.isEmptyProtectionEnabled());
		connectorConfig.setEmptyProtectionExpiredInterval(polarisConfigProperties.getEmptyProtectionExpiredInterval());

		// config report client address
		ServerConnectorConfigImpl reportClientConnectorConfig = configuration.getGlobal().getServerConnector();
		if (TsfContextUtils.isOnlyTsfConsulEnabled()) {
			configuration.getGlobal().getAPI().setReportEnable(false);
		}
		if (StringUtils.isNotBlank(polarisContextProperties.getAddress())) {
			reportClientConnectorConfig.setAddresses(AddressUtils.parseAddressList(polarisContextProperties.getAddress()));
		}
		else {
			reportClientConnectorConfig.setAddresses(resolvePolarisAddressFromConfigAddress(polarisConfigProperties.getAddress()));
		}
		reportClientConnectorConfig.setLbPolicy(polarisContextProperties.getAddressLbPolicy());
		reportClientConnectorConfig.setServerSwitchInterval(polarisContextProperties.getServerSwitchInterval());

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
			if (StringUtils.isNotBlank(address)) {
				int pos = address.lastIndexOf(":");
				if (pos != -1) {
					configAddresses.add(address.substring(0, pos) + ":" + polarisConfigProperties.getPort());
				}
				else {
					configAddresses.add(address);
				}
			}
		}

		return configAddresses;
	}

	private List<String> resolvePolarisAddressFromConfigAddress(String configAddress) {
		if (StringUtils.isEmpty(configAddress)) {
			return null;
		}

		List<String> configAddresses = AddressUtils.parseAddressList(configAddress);
		List<String> polarisAddresses = new ArrayList<>(configAddresses.size());

		for (String address : configAddresses) {
			if (StringUtils.isNotBlank(address)) {
				int pos = address.lastIndexOf(":");
				if (pos != -1) {
					polarisAddresses.add(address.substring(0, pos) + ":8091");
				}
				else {
					polarisAddresses.add(address);
				}
			}
		}

		return polarisAddresses;
	}

	private void checkAddressAccessible(List<String> configAddresses) {
		// check address can connect
		configAddresses.forEach(address -> {
			String[] ipPort;
			// check ipv6 address, format: [ipv6_ip]:port
			if (address.contains("[") && address.contains("]")) {
				int lastColonIndex = address.lastIndexOf(':');
				String port = address.substring(lastColonIndex + 1);
				String ip = address.substring(1, lastColonIndex - 1);
				ipPort = new String[]{ip, port};
			}
			else {
				ipPort = address.split(":");
			}

			if (ipPort.length != 2) {
				throw new IllegalArgumentException("Config server address (" + address + ") is wrong, please check address like grpc://127.0.0.1:8091.");
			}

			LOGGER.info("[SCT] Check config server ipPort: {}", Arrays.asList(ipPort));

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
