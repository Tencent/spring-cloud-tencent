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

package com.tencent.cloud.polaris.extend.consul;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.config.extend.consul.ConsulProperties;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfCoreProperties;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.DiscoveryConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.config.provider.RegisterConfigImpl;
import com.tencent.polaris.plugins.connector.common.constant.ConsulConstant;
import io.micrometer.common.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

/**
 * Modifier for Consul discovery.
 *
 * @author lingxiao.wlx, Haotian Zhang
 */
public class ConsulDiscoveryConfigModifier implements PolarisConfigModifier {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsulDiscoveryConfigModifier.class);

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	private final ConsulProperties consulProperties;

	private final ConsulDiscoveryProperties consulDiscoveryProperties;

	private final ConsulHeartbeatProperties consulHeartbeatProperties;

	private final ApplicationContext context;

	private final TsfCoreProperties tsfCoreProperties;

	public ConsulDiscoveryConfigModifier(PolarisDiscoveryProperties polarisDiscoveryProperties, ConsulProperties consulProperties,
			ConsulDiscoveryProperties consulDiscoveryProperties, ConsulHeartbeatProperties consulHeartbeatProperties,
			@Nullable TsfCoreProperties tsfCoreProperties, ApplicationContext context) {
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
		this.consulProperties = consulProperties;
		this.consulDiscoveryProperties = consulDiscoveryProperties;
		this.consulHeartbeatProperties = consulHeartbeatProperties;
		this.tsfCoreProperties = tsfCoreProperties;
		this.context = context;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		// init server connectors.
		if (CollectionUtils.isEmpty(configuration.getGlobal().getServerConnectors())) {
			configuration.getGlobal().setServerConnectors(new ArrayList<>());
		}

		// init consul connector.
		ServerConnectorConfigImpl serverConnectorConfig = new ServerConnectorConfigImpl();
		serverConnectorConfig.setId(ConsulDiscoveryUtil.ID);
		serverConnectorConfig.setAddresses(
				Collections.singletonList(consulProperties.getHost() + ":" + consulProperties.getPort()));
		LOGGER.info("Will register to consul server: [" + consulProperties.getHost() + ":" + consulProperties.getPort() + "]");
		serverConnectorConfig.setProtocol(DefaultPlugins.SERVER_CONNECTOR_CONSUL);
		// set consul connector metadata.
		Map<String, String> metadata = serverConnectorConfig.getMetadata();
		// namespace
		if (StringUtils.isNotBlank(polarisDiscoveryProperties.getNamespace())) {
			metadata.put(ConsulConstant.MetadataMapKey.NAMESPACE_KEY, polarisDiscoveryProperties.getNamespace());
		}
		// service name
		String appName = ConsulDiscoveryUtil.getAppName(consulDiscoveryProperties, context.getEnvironment());
		if (StringUtils.isNotBlank(appName)) {
			metadata.put(ConsulConstant.MetadataMapKey.SERVICE_NAME_KEY, ConsulDiscoveryUtil.normalizeForDns(appName));
		}
		// instance ID
		String instanceId = ConsulDiscoveryUtil.getInstanceId(consulDiscoveryProperties, context);
		if (StringUtils.isNotBlank(instanceId)) {
			metadata.put(ConsulConstant.MetadataMapKey.INSTANCE_ID_KEY, instanceId);
		}
		// token
		if (StringUtils.isNotBlank(consulProperties.getAclToken())) {
			serverConnectorConfig.setToken(consulProperties.getAclToken());
		}
		// default query tag
		if (StringUtils.isNotBlank(consulDiscoveryProperties.getDefaultQueryTag())) {
			metadata.put(ConsulConstant.MetadataMapKey.QUERY_TAG_KEY, consulDiscoveryProperties.getDefaultQueryTag());
		}
		// query passing
		metadata.put(ConsulConstant.MetadataMapKey.QUERY_PASSING_KEY, String.valueOf(consulDiscoveryProperties.isQueryPassing()));
		// prefer ip address
		if (consulDiscoveryProperties.isPreferIpAddress()
				&& StringUtils.isNotBlank(consulDiscoveryProperties.getIpAddress())) {
			metadata.put(ConsulConstant.MetadataMapKey.PREFER_IP_ADDRESS_KEY,
					String.valueOf(consulDiscoveryProperties.isPreferIpAddress()));
			metadata.put(ConsulConstant.MetadataMapKey.IP_ADDRESS_KEY, consulDiscoveryProperties.getIpAddress());
		}
		// is not prefer agent address
		if (!consulDiscoveryProperties.isPreferAgentAddress()) {
			metadata.put(ConsulConstant.MetadataMapKey.PREFER_IP_ADDRESS_KEY,
					String.valueOf(consulDiscoveryProperties.isPreferIpAddress()));
			metadata.put(ConsulConstant.MetadataMapKey.IP_ADDRESS_KEY, consulDiscoveryProperties.getHostname());
		}
		if (tsfCoreProperties != null) {
			// tags
			metadata.put(ConsulConstant.MetadataMapKey.TAGS_KEY, JacksonUtils.serialize2Json(tsfCoreProperties.getTsfTags()));
		}
		configuration.getGlobal().getServerConnectors().add(serverConnectorConfig);

		// discovery
		DiscoveryConfigImpl discoveryConfig = new DiscoveryConfigImpl();
		discoveryConfig.setServerConnectorId(ConsulDiscoveryUtil.ID);
		discoveryConfig.setEnable(consulDiscoveryProperties.isEnabled());
		configuration.getConsumer().getDiscoveries().add(discoveryConfig);

		// register
		RegisterConfigImpl registerConfig = new RegisterConfigImpl();
		registerConfig.setServerConnectorId(ConsulDiscoveryUtil.ID);
		registerConfig.setEnable(consulDiscoveryProperties.isRegister());
		configuration.getProvider().getRegisters().add(registerConfig);

		// heartbeat
		polarisDiscoveryProperties.setHeartbeatInterval(
				(int) consulHeartbeatProperties.computeHeartbeatInterval().getSeconds());
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.CONSUL_DISCOVERY_CONFIG_ORDER;
	}
}
