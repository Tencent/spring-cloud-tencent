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

package com.tencent.cloud.polaris.extend.consul;

import java.util.List;
import java.util.Map;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.INSTANCE_ID_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.IP_ADDRESS_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.PREFER_IP_ADDRESS_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.SERVICE_NAME_KEY;
import static com.tencent.polaris.test.common.Consts.HOST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConsulDiscoveryPropertiesTest}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ConsulDiscoveryPropertiesTest.TestApplication.class)
@ActiveProfiles("test")
public class ConsulDiscoveryPropertiesTest {

	@Autowired
	private ConsulDiscoveryProperties consulDiscoveryProperties;

	@Autowired
	private PolarisSDKContextManager polarisSDKContextManager;

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	public void testDefaultInitialization() {
		assertThat(consulDiscoveryProperties).isNotNull();
		assertThat(consulDiscoveryProperties.isRegister()).isTrue();
		assertThat(consulDiscoveryProperties.isEnabled()).isTrue();
	}

	@Test
	public void testModify() {
		assertThat(polarisSDKContextManager).isNotNull();
		com.tencent.polaris.api.config.Configuration configuration = polarisSDKContextManager.getSDKContext()
				.getConfig();
		List<ServerConnectorConfigImpl> serverConnectorConfigs = configuration.getGlobal().getServerConnectors();
		Map<String, String> metadata = null;
		for (ServerConnectorConfigImpl serverConnectorConfig : serverConnectorConfigs) {
			if (serverConnectorConfig.getId().equals("consul")) {
				metadata = serverConnectorConfig.getMetadata();
			}
		}
		assertThat(metadata).isNotNull();
		assertThat(metadata.get(SERVICE_NAME_KEY)).isEqualTo("java-provider-test");
		assertThat(metadata.get(INSTANCE_ID_KEY)).isEqualTo("ins-test");
		assertThat(metadata.get(PREFER_IP_ADDRESS_KEY)).isEqualTo("true");
		assertThat(metadata.get(IP_ADDRESS_KEY)).isEqualTo(HOST);
	}

	@SpringBootApplication
	protected static class TestApplication {

		static {
			PolarisSDKContextManager.innerDestroy();
		}
	}
}
