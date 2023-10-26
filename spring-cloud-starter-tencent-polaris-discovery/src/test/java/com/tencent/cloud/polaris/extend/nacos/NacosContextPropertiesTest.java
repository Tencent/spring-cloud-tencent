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

package com.tencent.cloud.polaris.extend.nacos;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link NacosContextProperties}.
 *
 * @author lingxiao.wlx
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = NacosContextPropertiesTest.TestApplication.class)
@ActiveProfiles("test")
public class NacosContextPropertiesTest {

	@Autowired
	private NacosContextProperties nacosContextProperties;

	@Autowired
	private PolarisSDKContextManager polarisSDKContextManager;

	@Test
	public void testDefaultInitialization() {
		assertThat(nacosContextProperties).isNotNull();
		assertThat(nacosContextProperties.isEnabled()).isTrue();
		assertThat(nacosContextProperties.getServerAddr()).isEqualTo("127.0.0.1:8848");
		assertThat(nacosContextProperties.isRegisterEnabled()).isTrue();
		assertThat(nacosContextProperties.isDiscoveryEnabled()).isTrue();
		assertThat(nacosContextProperties.getGroup()).isNotBlank();
		assertThat(nacosContextProperties.getClusterName()).isNotBlank();
		assertThat(nacosContextProperties.getNamespace()).isNotBlank();
	}

	@Test
	public void testModify() {
		assertThat(polarisSDKContextManager).isNotNull();
		com.tencent.polaris.api.config.Configuration configuration = polarisSDKContextManager.getSDKContext()
				.getConfig();
		List<ServerConnectorConfigImpl> serverConnectorConfigs = configuration.getGlobal().getServerConnectors();
		Optional<ServerConnectorConfigImpl> optionalServerConnectorConfig = serverConnectorConfigs.stream().filter(
				item -> "nacos".equals(item.getId())
		).findAny();
		assertThat(optionalServerConnectorConfig.isPresent()).isTrue();
		ServerConnectorConfigImpl serverConnectorConfig = optionalServerConnectorConfig.get();
		if (!CollectionUtils.isEmpty(serverConnectorConfig.getAddresses())) {
			assertThat(nacosContextProperties.getServerAddr()
					.equals(serverConnectorConfig.getAddresses().get(0))).isTrue();
		}
		assertThat(DefaultPlugins.SERVER_CONNECTOR_NACOS.equals(serverConnectorConfig.getProtocol())).isTrue();

		Map<String, String> metadata = serverConnectorConfig.getMetadata();
		assertThat(metadata.get(NacosConfigModifier.USERNAME)).isEqualTo(nacosContextProperties.getUsername());
		assertThat(metadata.get(NacosConfigModifier.PASSWORD)).isEqualTo(nacosContextProperties.getPassword());
		assertThat(metadata.get(NacosConfigModifier.CONTEXT_PATH)).isEqualTo(nacosContextProperties.getContextPath());
		assertThat(metadata.get(NacosConfigModifier.NAMESPACE)).isEqualTo(nacosContextProperties.getNamespace());
		assertThat(metadata.get(NacosConfigModifier.GROUP)).isEqualTo(nacosContextProperties.getGroup());
	}

	@SpringBootApplication
	protected static class TestApplication {

		static {
			PolarisSDKContextManager.innerDestroy();
		}
	}
}
