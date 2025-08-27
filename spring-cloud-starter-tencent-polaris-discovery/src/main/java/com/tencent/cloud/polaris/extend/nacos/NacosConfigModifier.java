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

package com.tencent.cloud.polaris.extend.nacos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.DiscoveryConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.config.provider.RegisterConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_CLUSTER_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_EPHEMERAL_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_GROUP_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_SERVICE_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_WEIGHT_KEY;
import static shade.polaris.com.alibaba.nacos.api.PropertyKeyConst.CONTEXT_PATH;
import static shade.polaris.com.alibaba.nacos.api.PropertyKeyConst.NAMESPACE;
import static shade.polaris.com.alibaba.nacos.api.PropertyKeyConst.PASSWORD;
import static shade.polaris.com.alibaba.nacos.api.PropertyKeyConst.USERNAME;

/**
 * {@link PolarisConfigModifier} impl of Nacos.
 *
 * @author lingxiao.wlx
 */
public class NacosConfigModifier implements PolarisConfigModifier {


	private static final Logger LOGGER = LoggerFactory.getLogger(NacosConfigModifier.class);
	private static final String ID = "nacos";
	private final NacosContextProperties nacosContextProperties;

	public NacosConfigModifier(NacosContextProperties nacosContextProperties) {
		this.nacosContextProperties = nacosContextProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		if (Objects.isNull(nacosContextProperties) || !nacosContextProperties.isDiscoveryEnabled()) {
			return;
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
		if (StringUtils.isBlank(nacosContextProperties.getServerAddr())) {
			throw new IllegalArgumentException("nacos server addr must not be empty, please set it by" +
					"spring.cloud.nacos.discovery.server-addr");
		}
		serverConnectorConfig.setAddresses(
				Collections.singletonList(nacosContextProperties.getServerAddr()));
		serverConnectorConfig.setProtocol(DefaultPlugins.SERVER_CONNECTOR_NACOS);

		Map<String, String> metadata = serverConnectorConfig.getMetadata();
		if (StringUtils.isNotBlank(nacosContextProperties.getUsername())) {
			metadata.put(USERNAME, nacosContextProperties.getUsername());
		}

		if (StringUtils.isNotBlank(nacosContextProperties.getPassword())) {
			metadata.put(PASSWORD, nacosContextProperties.getPassword());
		}

		if (StringUtils.isNotBlank(nacosContextProperties.getContextPath())) {
			metadata.put(CONTEXT_PATH, nacosContextProperties.getContextPath());
		}

		if (StringUtils.isNotBlank(nacosContextProperties.getNamespace())) {
			metadata.put(NAMESPACE, nacosContextProperties.getNamespace());
		}

		if (StringUtils.isNotBlank(nacosContextProperties.getGroup())) {
			metadata.put(NACOS_GROUP_KEY, nacosContextProperties.getGroup());
		}
		if (StringUtils.isNotBlank(nacosContextProperties.getClusterName())) {
			metadata.put(NACOS_CLUSTER_KEY, nacosContextProperties.getClusterName());
		}
		if (StringUtils.isNotBlank(nacosContextProperties.getServiceName())) {
			metadata.put(NACOS_SERVICE_KEY, nacosContextProperties.getServiceName());
		}
		metadata.put(NACOS_EPHEMERAL_KEY, String.valueOf(nacosContextProperties.isEphemeral()));
		metadata.put(NACOS_WEIGHT_KEY, String.valueOf(nacosContextProperties.getWeight()));
		configuration.getGlobal().getServerConnectors().add(serverConnectorConfig);
		DiscoveryConfigImpl discoveryConfig = new DiscoveryConfigImpl();
		discoveryConfig.setServerConnectorId(ID);
		discoveryConfig.setEnable(nacosContextProperties.isDiscoveryEnabled());
		configuration.getConsumer().getDiscoveries().add(discoveryConfig);

		RegisterConfigImpl registerConfig = new RegisterConfigImpl();
		registerConfig.setServerConnectorId(ID);
		registerConfig.setEnable(nacosContextProperties.isRegisterEnabled());
		configuration.getProvider().getRegisters().add(registerConfig);
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.NACOS_DISCOVERY_CONFIG_ORDER;
	}
}
