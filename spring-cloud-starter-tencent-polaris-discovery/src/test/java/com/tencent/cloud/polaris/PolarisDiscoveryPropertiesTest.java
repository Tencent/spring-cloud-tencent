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

package com.tencent.cloud.polaris;

import org.junit.Test;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.PROVIDER_TOKEN;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisDiscoveryProperties}.
 *
 * @author Haotian Zhang
 */
public class PolarisDiscoveryPropertiesTest {

	@Test
	public void testGetAndSet() {
		PolarisDiscoveryProperties polarisDiscoveryProperties = new PolarisDiscoveryProperties();

		// HeartbeatEnabled
		polarisDiscoveryProperties.setHeartbeatEnabled(true);
		assertThat(polarisDiscoveryProperties.isHeartbeatEnabled()).isTrue();

		// HeartbeatEnabled
		polarisDiscoveryProperties.setHeartbeatInterval(200);
		assertThat(polarisDiscoveryProperties.getHeartbeatInterval()).isEqualTo(5);
		polarisDiscoveryProperties.setHeartbeatInterval(0);
		assertThat(polarisDiscoveryProperties.getHeartbeatInterval()).isEqualTo(5);
		polarisDiscoveryProperties.setHeartbeatInterval(20);
		assertThat(polarisDiscoveryProperties.getHeartbeatInterval()).isEqualTo(20);

		// Namespace
		polarisDiscoveryProperties.setNamespace(NAMESPACE_TEST);
		assertThat(polarisDiscoveryProperties.getNamespace()).isEqualTo(NAMESPACE_TEST);

		// Weight
		polarisDiscoveryProperties.setWeight(10);
		assertThat(polarisDiscoveryProperties.getWeight()).isEqualTo(10);

		// Service
		polarisDiscoveryProperties.setService(SERVICE_PROVIDER);
		assertThat(polarisDiscoveryProperties.getService()).isEqualTo(SERVICE_PROVIDER);

		// Enabled
		polarisDiscoveryProperties.setEnabled(true);
		assertThat(polarisDiscoveryProperties.isEnabled()).isTrue();

		// RegisterEnabled
		polarisDiscoveryProperties.setRegisterEnabled(true);
		assertThat(polarisDiscoveryProperties.isRegisterEnabled()).isTrue();

		// Token
		polarisDiscoveryProperties.setToken(PROVIDER_TOKEN);
		assertThat(polarisDiscoveryProperties.getToken()).isEqualTo(PROVIDER_TOKEN);

		// Version
		polarisDiscoveryProperties.setVersion("1.0.0");
		assertThat(polarisDiscoveryProperties.getVersion()).isEqualTo("1.0.0");

		// HTTP
		polarisDiscoveryProperties.setProtocol("HTTP");
		assertThat(polarisDiscoveryProperties.getProtocol()).isEqualTo("HTTP");

		// Port
		polarisDiscoveryProperties.setPort(PORT);
		assertThat(polarisDiscoveryProperties.getPort()).isEqualTo(PORT);

		// HealthCheckUrl
		polarisDiscoveryProperties.setHealthCheckUrl("/health");
		assertThat(polarisDiscoveryProperties.getHealthCheckUrl()).isEqualTo("/health");

		// ServiceListRefreshInterval
		polarisDiscoveryProperties.setServiceListRefreshInterval(1000L);
		assertThat(polarisDiscoveryProperties.getServiceListRefreshInterval()).isEqualTo(1000L);

		assertThat(polarisDiscoveryProperties.toString())
				.isEqualTo("PolarisDiscoveryProperties{"
						+ "namespace='Test'"
						+ ", service='java_provider_test'"
						+ ", token='19485a7674294e3c88dba293373c1534'"
						+ ", weight=10, version='1.0.0'"
						+ ", protocol='HTTP'"
						+ ", port=9091"
						+ ", enabled=true"
						+ ", registerEnabled=true"
						+ ", heartbeatEnabled=true"
						+ ", heartbeatInterval=20"
						+ ", healthCheckUrl='/health'"
						+ ", serviceListRefreshInterval=1000}");
	}
}
