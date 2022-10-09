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
 */

package com.tencent.cloud.polaris.extend.consul;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import com.tencent.cloud.common.constant.ContextConstant.ModifierOrder;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.DiscoveryConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.config.provider.RegisterConfigImpl;
import com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey;
import org.apache.commons.lang.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.CollectionUtils;

/**
 * Discovery configuration of Consul.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties("spring.cloud.consul")
public class ConsulContextProperties {

	/**
	 * Host of consul(or consul agent).
	 */
	private String host;

	private int port;

	private boolean enabled = false;

	@Value("${spring.cloud.consul.discovery.register:#{'true'}}")
	private boolean register;

	@Value("${spring.cloud.consul.discovery.enabled:#{'true'}}")
	private boolean discoveryEnabled;

	@Value("${spring.cloud.consul.discovery.instance-id:}")
	private String instanceId;

	@Value("${spring.cloud.consul.discovery.service-name:${spring.application.name:}}")
	private String serviceName;

	@Value("${spring.cloud.consul.discovery.ip-address:}")
	private String ipAddress;

	@Value("${spring.cloud.consul.discovery.prefer-ip-address:#{'false'}}")
	private boolean preferIpAddress;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isRegister() {
		return register;
	}

	public boolean isDiscoveryEnabled() {
		return discoveryEnabled;
	}

	@Bean
	@ConditionalOnMissingBean
	public ConsulConfigModifier consulConfigModifier() {
		return new ConsulConfigModifier();
	}

	private static class ConsulConfigModifier implements PolarisConfigModifier {

		private static final String ID = "consul";

		@Autowired(required = false)
		private ConsulContextProperties consulContextProperties;

		@Override
		public void modify(ConfigurationImpl configuration) {
			if (consulContextProperties != null && consulContextProperties.enabled) {
				if (CollectionUtils.isEmpty(configuration.getGlobal().getServerConnectors())) {
					configuration.getGlobal().setServerConnectors(new ArrayList<>());
				}
				if (CollectionUtils.isEmpty(configuration.getGlobal().getServerConnectors())
						&& null != configuration.getGlobal().getServerConnector()) {
					configuration.getGlobal().getServerConnectors().add(configuration.getGlobal().getServerConnector());
				}
				ServerConnectorConfigImpl serverConnectorConfig = new ServerConnectorConfigImpl();
				serverConnectorConfig.setId(ID);
				serverConnectorConfig.setAddresses(Collections.singletonList(consulContextProperties.host + ":"
						+ consulContextProperties.port));
				serverConnectorConfig.setProtocol(DefaultPlugins.SERVER_CONNECTOR_CONSUL);
				Map<String, String> metadata = serverConnectorConfig.getMetadata();
				if (StringUtils.isNotBlank(consulContextProperties.serviceName)) {
					metadata.put(MetadataMapKey.SERVICE_NAME_KEY, consulContextProperties.serviceName);
				}
				if (StringUtils.isNotBlank(consulContextProperties.instanceId)) {
					metadata.put(MetadataMapKey.INSTANCE_ID_KEY, consulContextProperties.instanceId);
				}
				if (consulContextProperties.preferIpAddress
						&& StringUtils.isNotBlank(consulContextProperties.ipAddress)) {
					metadata.put(MetadataMapKey.PREFER_IP_ADDRESS_KEY,
							String.valueOf(consulContextProperties.preferIpAddress));
					metadata.put(MetadataMapKey.IP_ADDRESS_KEY, consulContextProperties.ipAddress);
				}
				configuration.getGlobal().getServerConnectors().add(serverConnectorConfig);

				DiscoveryConfigImpl discoveryConfig = new DiscoveryConfigImpl();
				discoveryConfig.setServerConnectorId(ID);
				discoveryConfig.setEnable(consulContextProperties.discoveryEnabled);
				configuration.getConsumer().getDiscoveries().add(discoveryConfig);

				RegisterConfigImpl registerConfig = new RegisterConfigImpl();
				registerConfig.setServerConnectorId(ID);
				registerConfig.setEnable(consulContextProperties.register);
				configuration.getProvider().getRegisters().add(registerConfig);
			}
		}

		@Override
		public int getOrder() {
			return ModifierOrder.LAST;
		}
	}
}
