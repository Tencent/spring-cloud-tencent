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

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link NacosContextProperties} with polaris agent property.
 *
 * @author fishtailfu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
		classes = NacosContextPropertiesAgentTest.TestApplication.class,
		properties = {
				"spring.cloud.polaris.address=grpc://127.0.0.1:8091",
				"spring.cloud.polaris.discovery.enabled=false",
				"polaris.agent.nacos.discovery.enabled=true",
				"spring.cloud.nacos.discovery.enabled=false",
				"spring.cloud.nacos.server-addr=127.0.0.1:8848",
				"spring.cloud.nacos.discovery.register-enabled=true",
				"spring.cloud.nacos.discovery.group=polaris",
				"spring.cloud.nacos.username=nacos",
				"spring.cloud.nacos.password=nacos",
				"spring.cloud.nacos.discovery.cluster-name=polaris",
				"spring.cloud.nacos.context-path=/nacos"
		}
)
@ActiveProfiles("agent-test")
public class NacosContextPropertiesAgentTest {

	@Autowired
	private NacosContextProperties nacosContextProperties;

	@Test
	public void testPolarisAgentNacosDiscoveryEnabled() {
		assertThat(nacosContextProperties).isNotNull();
		// Test that polaris.agent.nacos.discovery.enabled takes precedence
		assertThat(nacosContextProperties.isDiscoveryEnabled()).isTrue();
		assertThat(nacosContextProperties.getServerAddr()).isEqualTo("127.0.0.1:8848");
		assertThat(nacosContextProperties.isRegisterEnabled()).isTrue();
		assertThat(nacosContextProperties.getGroup()).isEqualTo("polaris");
		assertThat(nacosContextProperties.getUsername()).isEqualTo("nacos");
		assertThat(nacosContextProperties.getPassword()).isEqualTo("nacos");
		assertThat(nacosContextProperties.getClusterName()).isEqualTo("polaris");
		assertThat(nacosContextProperties.getContextPath()).isEqualTo("/nacos");
	}

	@SpringBootApplication
	protected static class TestApplication {

		static {
			PolarisSDKContextManager.innerDestroy();
		}
	}
}
