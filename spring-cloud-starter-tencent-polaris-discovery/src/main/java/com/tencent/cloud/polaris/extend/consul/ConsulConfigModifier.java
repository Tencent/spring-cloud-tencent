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

package com.tencent.cloud.polaris.extend.consul;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.DiscoveryConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.config.provider.RegisterConfigImpl;
import com.tencent.polaris.plugins.connector.common.constant.ConsulConstant;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.CollectionUtils;

/**
 * @author lingxiao.wlx
 */
public class ConsulConfigModifier implements PolarisConfigModifier {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsulConfigModifier.class);

	private static final String ID = "consul";

	private final ConsulContextProperties consulContextProperties;

	public ConsulConfigModifier(ConsulContextProperties consulContextProperties) {
		this.consulContextProperties = consulContextProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		if (consulContextProperties != null && consulContextProperties.isEnabled()) {
			// Check if Consul client Available
			boolean consulAvailable = false;
			try {
				consulAvailable = null != Class.forName("com.ecwid.consul.v1.ConsulClient");
			}
			catch (Throwable ignored) {

			}
			if (!consulAvailable) {
				LOGGER.error("Please import \"connector-consul\" dependency when enabling consul service registration and discovery.\n"
						+ "Add dependency configuration below to pom.xml:\n"
						+ "<dependency>\n"
						+ "\t<groupId>com.tencent.polaris</groupId>\n"
						+ "\t<artifactId>connector-consul</artifactId>\n"
						+ "</dependency>");
				throw new RuntimeException("Dependency \"connector-consul\" not found.");
			}
			if (CollectionUtils.isEmpty(configuration.getGlobal().getServerConnectors())) {
				configuration.getGlobal().setServerConnectors(new ArrayList<>());
			}
			if (CollectionUtils.isEmpty(configuration.getGlobal().getServerConnectors())
					&& null != configuration.getGlobal().getServerConnector()) {
				configuration.getGlobal().getServerConnectors().add(configuration.getGlobal().getServerConnector());
			}
			ServerConnectorConfigImpl serverConnectorConfig = new ServerConnectorConfigImpl();
			serverConnectorConfig.setId(ID);
			serverConnectorConfig.setAddresses(
					Collections.singletonList(consulContextProperties.getHost() + ":" + consulContextProperties.getPort()));
			serverConnectorConfig.setProtocol(DefaultPlugins.SERVER_CONNECTOR_CONSUL);
			Map<String, String> metadata = serverConnectorConfig.getMetadata();
			if (StringUtils.isNotBlank(consulContextProperties.getServiceName())) {
				metadata.put(ConsulConstant.MetadataMapKey.SERVICE_NAME_KEY, consulContextProperties.getServiceName());
			}
			if (StringUtils.isNotBlank(consulContextProperties.getInstanceId())) {
				metadata.put(ConsulConstant.MetadataMapKey.INSTANCE_ID_KEY, consulContextProperties.getInstanceId());
			}
			if (consulContextProperties.isPreferIpAddress()
					&& StringUtils.isNotBlank(consulContextProperties.getIpAddress())) {
				metadata.put(ConsulConstant.MetadataMapKey.PREFER_IP_ADDRESS_KEY,
						String.valueOf(consulContextProperties.isPreferIpAddress()));
				metadata.put(ConsulConstant.MetadataMapKey.IP_ADDRESS_KEY, consulContextProperties.getIpAddress());
			}
			configuration.getGlobal().getServerConnectors().add(serverConnectorConfig);

			DiscoveryConfigImpl discoveryConfig = new DiscoveryConfigImpl();
			discoveryConfig.setServerConnectorId(ID);
			discoveryConfig.setEnable(consulContextProperties.isDiscoveryEnabled());
			configuration.getConsumer().getDiscoveries().add(discoveryConfig);

			RegisterConfigImpl registerConfig = new RegisterConfigImpl();
			registerConfig.setServerConnectorId(ID);
			registerConfig.setEnable(consulContextProperties.isRegister());
			configuration.getProvider().getRegisters().add(registerConfig);
		}
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.CONSUL_DISCOVERY_CONFIG_ORDER;
	}
}
