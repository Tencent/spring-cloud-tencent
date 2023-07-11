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
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.extend.consul.ConsulContextProperties;
import com.tencent.cloud.polaris.extend.nacos.NacosContextProperties;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.APIConfig;
import com.tencent.polaris.api.config.global.GlobalConfig;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import static com.tencent.polaris.test.common.Consts.HOST;
import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisRegistration}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PolarisRegistrationTest {

	private static final int testLocalPort = 10086;
	private NacosContextProperties nacosContextProperties;
	private PolarisRegistration polarisRegistration1;
	private PolarisRegistration polarisRegistration2;
	private PolarisRegistration polarisRegistration3;
	private PolarisRegistration polarisRegistration4;

	@BeforeEach
	void setUp() {
		// mock PolarisDiscoveryProperties
		PolarisDiscoveryProperties polarisDiscoveryProperties = mock(PolarisDiscoveryProperties.class);
		doReturn(SERVICE_PROVIDER).when(polarisDiscoveryProperties).getService();
		doReturn("http").when(polarisDiscoveryProperties).getProtocol();
		doReturn(true).when(polarisDiscoveryProperties).isRegisterEnabled();

		// mock PolarisContextProperties
		PolarisContextProperties polarisContextProperties = mock(PolarisContextProperties.class);
		doReturn(testLocalPort).when(polarisContextProperties).getLocalPort();

		// mock ConsulContextProperties
		ConsulContextProperties consulContextProperties = mock(ConsulContextProperties.class);
		doReturn(true).when(consulContextProperties).isEnabled();
		doReturn(true).when(consulContextProperties).isRegister();

		// mock NacosContextProperties
		nacosContextProperties = mock(NacosContextProperties.class);
		doReturn(true).when(nacosContextProperties).isEnabled();
		doReturn(true).when(nacosContextProperties).isRegisterEnabled();
		doReturn("/").when(nacosContextProperties).getContextPath();
		doReturn("cluster").when(nacosContextProperties).getClusterName();
		doReturn("").when(nacosContextProperties).getGroup();
		doReturn(true).when(nacosContextProperties).isDiscoveryEnabled();
		doReturn("").when(nacosContextProperties).getPassword();
		doReturn("").when(nacosContextProperties).getUsername();
		doReturn("").when(nacosContextProperties).getServerAddr();

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

		// mock ServletWebServerApplicationContext
		WebServer servletwebServer = mock(WebServer.class);
		doReturn(PORT).when(servletwebServer).getPort();
		ServletWebServerApplicationContext servletWebServerApplicationContext = mock(ServletWebServerApplicationContext.class);
		doReturn(servletwebServer).when(servletWebServerApplicationContext).getWebServer();

		// mock ReactiveWebServerApplicationContext
		WebServer reactiveWebServer = mock(WebServer.class);
		doReturn(PORT + 1).when(reactiveWebServer).getPort();
		ReactiveWebServerApplicationContext reactiveWebServerApplicationContext = mock(ReactiveWebServerApplicationContext.class);
		doReturn(reactiveWebServer).when(reactiveWebServerApplicationContext).getWebServer();

		polarisRegistration1 = PolarisRegistration.registration(polarisDiscoveryProperties, null, consulContextProperties,
				polarisContext, staticMetadataManager, nacosContextProperties,
				servletWebServerApplicationContext, null, null);

		polarisRegistration2 = PolarisRegistration.registration(polarisDiscoveryProperties, null, consulContextProperties,
				polarisContext, staticMetadataManager, nacosContextProperties,
				null, reactiveWebServerApplicationContext, null);

		polarisRegistration3 = PolarisRegistration.registration(polarisDiscoveryProperties, null, consulContextProperties,
				polarisContext, staticMetadataManager, nacosContextProperties,
				null, null, null);

		polarisRegistration4 = PolarisRegistration.registration(polarisDiscoveryProperties, polarisContextProperties, consulContextProperties,
				polarisContext, staticMetadataManager, nacosContextProperties,
				null, null, null);
	}

	@Test
	public void testGetServiceId() {
		assertThat(polarisRegistration1.getServiceId()).isEqualTo(SERVICE_PROVIDER);
	}

	@Test
	public void testGetHost() {
		assertThat(polarisRegistration1.getHost()).isEqualTo(HOST);
	}

	@Test
	public void testGetPort() {
		assertThat(polarisRegistration1.getPort()).isEqualTo(PORT);
		assertThat(polarisRegistration2.getPort()).isEqualTo(PORT + 1);
		try {
			polarisRegistration3.getPort();
		}
		catch (RuntimeException e) {
			assertThat(e.getMessage()).isEqualTo("Unsupported web type.");
		}
		assertThat(polarisRegistration4.getPort()).isEqualTo(testLocalPort);
	}

	@Test
	public void testIsSecure() {
		assertThat(polarisRegistration1.isSecure()).isFalse();
	}

	@Test
	public void testGetUri() {
		assertThat(polarisRegistration1.getUri().toString()).isEqualTo("http://" + HOST + ":" + PORT);
	}

	@Test
	public void testInstanceId() {
		polarisRegistration1.setInstanceId("TEST");
		assertThat(polarisRegistration1.getInstanceId()).isEqualTo("TEST");
	}

	@Test
	public void testGetMetadata() {
		Map<String, String> metadata = polarisRegistration1.getMetadata();
		assertThat(metadata).isNotNull();
		assertThat(metadata).isNotEmpty();
		assertThat(metadata.size()).isEqualTo(4);
		assertThat(metadata.get("key1")).isEqualTo("value1");
	}

	@Test
	public void testIsRegisterEnabled() {
		assertThat(polarisRegistration1.isRegisterEnabled()).isTrue();
	}

	@Test
	public void testToString() {
		System.out.println(polarisRegistration1);
	}

	@Test
	public void testGetNacosServiceId() {
		String groupName = "group";
		String format = "%s__%s";
		when(nacosContextProperties.getGroup()).thenReturn(groupName);
		String serviceId = polarisRegistration1.getServiceId();
		assertThat(String.format(format, groupName, SERVICE_PROVIDER).equals(serviceId));
	}

	@Test
	public void testGetNacosMetadata() {
		String clusterName = "cluster";
		when(nacosContextProperties.getClusterName()).thenReturn(clusterName);
		Map<String, String> metadata = polarisRegistration1.getMetadata();
		assertThat(metadata).isNotNull();
		assertThat(metadata).isNotEmpty();
		assertThat(metadata.size()).isEqualTo(4);
		assertThat(metadata.get("nacos.cluster")).isEqualTo(clusterName);
	}
}
