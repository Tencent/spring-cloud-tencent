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

package com.tencent.cloud.polaris.registry;

import java.util.Collections;
import java.util.Map;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.extend.consul.ConsulContextProperties;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.APIConfig;
import com.tencent.polaris.api.config.global.GlobalConfig;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.tencent.polaris.test.common.Consts.HOST;
import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PolarisRegistration}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisRegistrationTest {

	private PolarisRegistration polarisRegistration;

	@Before
	public void setUp() {
		// mock PolarisDiscoveryProperties
		PolarisDiscoveryProperties polarisDiscoveryProperties = mock(PolarisDiscoveryProperties.class);
		doReturn(SERVICE_PROVIDER).when(polarisDiscoveryProperties).getService();
		doReturn(PORT).when(polarisDiscoveryProperties).getPort();
		doReturn("http").when(polarisDiscoveryProperties).getProtocol();
		doReturn(true).when(polarisDiscoveryProperties).isRegisterEnabled();

		// mock
		ConsulContextProperties consulContextProperties = mock(ConsulContextProperties.class);

		// mock SDKContext
		APIConfig apiConfig = mock(APIConfig.class);
		doReturn(HOST).when(apiConfig).getBindIP();
		GlobalConfig globalConfig = mock(GlobalConfig.class);
		doReturn(apiConfig).when(globalConfig).getAPI();
		Configuration configuration = mock(Configuration.class);
		doReturn(globalConfig).when(configuration).getGlobal();
		SDKContext polarisContext = mock(SDKContext.class);
		doReturn(configuration).when(polarisContext).getConfig();

		// mock StaticMetadataManager
		StaticMetadataManager staticMetadataManager = mock(StaticMetadataManager.class);
		doReturn(Collections.singletonMap("key1", "value1")).when(staticMetadataManager).getMergedStaticMetadata();

		polarisRegistration = new PolarisRegistration(polarisDiscoveryProperties, consulContextProperties,
				polarisContext, staticMetadataManager);
	}

	@Test
	public void testGetServiceId() {
		assertThat(polarisRegistration.getServiceId()).isEqualTo(SERVICE_PROVIDER);
	}

	@Test
	public void testGetHost() {
		assertThat(polarisRegistration.getHost()).isEqualTo(HOST);
	}

	@Test
	public void testGetPort() {
		assertThat(polarisRegistration.getPort()).isEqualTo(PORT);
	}

	@Test
	public void testIsSecure() {
		assertThat(polarisRegistration.isSecure()).isFalse();
	}

	@Test
	public void testGetUri() {
		assertThat(polarisRegistration.getUri().toString()).isEqualTo("http://" + HOST + ":" + PORT);
	}

	@Test
	public void testGetMetadata() {
		Map<String, String> metadata = polarisRegistration.getMetadata();
		assertThat(metadata).isNotNull();
		assertThat(metadata).isNotEmpty();
		assertThat(metadata.size()).isEqualTo(3);
		assertThat(metadata.get("key1")).isEqualTo("value1");
	}

	@Test
	public void testGetPolarisProperties() {
		assertThat(polarisRegistration.getPolarisProperties()).isNotNull();
	}

	@Test
	public void testIsRegisterEnabled() {
		assertThat(polarisRegistration.isRegisterEnabled()).isTrue();
	}

	@Test
	public void testToString() {
		System.out.println(polarisRegistration);
	}
}
