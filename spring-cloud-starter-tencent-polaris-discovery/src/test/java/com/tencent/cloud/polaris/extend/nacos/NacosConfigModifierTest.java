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

import java.util.Map;
import java.util.Optional;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.DiscoveryConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.config.provider.RegisterConfigImpl;
import org.junit.jupiter.api.Test;

import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_CLUSTER_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_EPHEMERAL_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_GROUP_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_SERVICE_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_WEIGHT_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static shade.polaris.com.alibaba.nacos.api.PropertyKeyConst.CONTEXT_PATH;
import static shade.polaris.com.alibaba.nacos.api.PropertyKeyConst.NAMESPACE;
import static shade.polaris.com.alibaba.nacos.api.PropertyKeyConst.PASSWORD;
import static shade.polaris.com.alibaba.nacos.api.PropertyKeyConst.USERNAME;

/**
 * Test for {@link NacosConfigModifier}.
 *
 * @author fishtailfu
 */
public class NacosConfigModifierTest {

	@Test
	public void testNullProperties() {
		ConfigurationImpl configuration = configuration();
		new NacosConfigModifier(null).modify(configuration);
		assertThat(nacosConnector(configuration)).isEmpty();
	}

	@Test
	public void testDiscoveryDisabled() {
		NacosContextProperties properties = new NacosContextProperties();
		properties.setDiscoveryEnabled(false);
		properties.setServerAddr("127.0.0.1:8848");

		ConfigurationImpl configuration = configuration();
		new NacosConfigModifier(properties).modify(configuration);

		assertThat(nacosConnector(configuration)).isEmpty();
	}

	@Test
	public void testBlankServerAddr() {
		NacosContextProperties properties = enabledProperties();
		properties.setServerAddr(" ");

		assertThatThrownBy(() -> new NacosConfigModifier(properties).modify(configuration()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("spring.cloud.nacos.discovery.server-addr");
	}

	@Test
	public void testModifyConnectorMetadata() {
		NacosContextProperties properties = enabledProperties();
		properties.setServerAddr("127.0.0.1:8848");
		properties.setUsername("nacos");
		properties.setPassword("nacos");
		properties.setContextPath("/nacos");
		properties.setNamespace("public");
		properties.setGroup("polaris");
		properties.setClusterName("polaris");
		properties.setServiceName("order-service");
		properties.setEphemeral(false);
		properties.setWeight(2.5D);
		properties.setRegisterEnabled(false);

		ConfigurationImpl configuration = configuration();
		NacosConfigModifier modifier = new NacosConfigModifier(properties);
		modifier.modify(configuration);

		ServerConnectorConfigImpl connector = configuration.getGlobal().getServerConnectors().stream()
				.filter(item -> "nacos".equals(item.getId()))
				.findFirst()
				.orElseThrow(AssertionError::new);
		assertThat(connector.getProtocol()).isEqualTo(DefaultPlugins.SERVER_CONNECTOR_NACOS);
		assertThat(connector.getAddresses()).containsExactly("127.0.0.1:8848");
		Map<String, String> metadata = connector.getMetadata();
		assertThat(metadata.get(USERNAME)).isEqualTo("nacos");
		assertThat(metadata.get(PASSWORD)).isEqualTo("nacos");
		assertThat(metadata.get(CONTEXT_PATH)).isEqualTo("/nacos");
		assertThat(metadata.get(NAMESPACE)).isEqualTo("public");
		assertThat(metadata.get(NACOS_GROUP_KEY)).isEqualTo("polaris");
		assertThat(metadata.get(NACOS_CLUSTER_KEY)).isEqualTo("polaris");
		assertThat(metadata.get(NACOS_SERVICE_KEY)).isEqualTo("order-service");
		assertThat(metadata.get(NACOS_EPHEMERAL_KEY)).isEqualTo("false");
		assertThat(metadata.get(NACOS_WEIGHT_KEY)).isEqualTo("2.5");

		DiscoveryConfigImpl discoveryConfig = configuration.getConsumer().getDiscoveries().stream()
				.filter(item -> "nacos".equals(item.getServerConnectorId()))
				.findFirst()
				.orElseThrow(AssertionError::new);
		assertThat(discoveryConfig.isEnable()).isTrue();
		RegisterConfigImpl registerConfig = configuration.getProvider().getRegisters().stream()
				.filter(item -> "nacos".equals(item.getServerConnectorId()))
				.findFirst()
				.orElseThrow(AssertionError::new);
		assertThat(registerConfig.isEnable()).isFalse();
		assertThat(modifier.getOrder()).isEqualTo(OrderConstant.Modifier.NACOS_DISCOVERY_CONFIG_ORDER);
	}

	private NacosContextProperties enabledProperties() {
		NacosContextProperties properties = new NacosContextProperties();
		properties.setDiscoveryEnabled(true);
		return properties;
	}

	private ConfigurationImpl configuration() {
		ConfigurationImpl configuration = new ConfigurationImpl();
		configuration.setDefault();
		return configuration;
	}

	private Optional<ServerConnectorConfigImpl> nacosConnector(ConfigurationImpl configuration) {
		if (configuration.getGlobal().getServerConnectors() == null) {
			return Optional.empty();
		}
		return configuration.getGlobal().getServerConnectors().stream()
				.filter(connector -> "nacos".equals(connector.getId()))
				.findFirst();
	}
}
