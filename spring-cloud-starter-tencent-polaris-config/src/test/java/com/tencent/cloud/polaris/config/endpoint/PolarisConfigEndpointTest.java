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

package com.tencent.cloud.polaris.config.endpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.polaris.config.PolarisConfigSDKContextManager;
import com.tencent.cloud.polaris.config.adapter.MockedConfigKVFile;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySource;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.client.internal.CompositeConfigFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for polaris config endpoint.
 *
 * @author shuiqingliu
 */
@ExtendWith(MockitoExtension.class)
public class PolarisConfigEndpointTest {

	private final String testNamespace = "testNamespace";
	private final String testServiceName = "testServiceName";
	private final String testFileName = "application.properties";

	@Mock
	private PolarisConfigProperties polarisConfigProperties;

	@BeforeEach
	public void setUp() {
		PolarisPropertySourceManager.clearPropertySources();
	}

	@AfterEach
	public void tearDown() {
		PolarisConfigSDKContextManager.innerConfigDestroy();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPolarisConfigEndpoint() {
		PolarisConfigProperties properties = new PolarisConfigProperties();
		properties.setToken("endpoint-must-not-expose-this-token");
		Map<String, Object> content = new HashMap<>();
		content.put("k1", "sensitive-value-one");
		content.put("k2", "sensitive-value-two");
		content.put("k3", "sensitive-value-three");
		MockedConfigKVFile file = new MockedConfigKVFile(content);
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, testServiceName, testFileName,
				file, content);
		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		PolarisConfigEndpoint endpoint = new PolarisConfigEndpoint(properties);
		Map<String, Object> info = endpoint.polarisConfig();
		assertThat(info.get("ClientId")).isNull();
		assertThat(info.get("PolarisConfigProperties")).isInstanceOf(Map.class);
		Map<String, Object> configProperties = (Map<String, Object>) info.get("PolarisConfigProperties");
		assertThat(configProperties).doesNotContainKey("token");
		List<Map<String, Object>> sources = (List<Map<String, Object>>) info.get("PolarisPropertySource");
		assertThat(sources).hasSize(1);
		assertThat(sources.get(0)).containsEntry("namespace", testNamespace)
				.containsEntry("group", testServiceName)
				.containsEntry("fileName", testFileName)
				.containsKey("propertyNames")
				.doesNotContainKeys("k1", "k2", "k3");
		assertThat((List<String>) sources.get(0).get("propertyNames")).containsExactlyInAnyOrder("k1", "k2", "k3");

		// Endpoint returns plain maps for actuator serialization and must not leak credentials
		// or property values into the diagnostic payload.
		assertThat(String.valueOf(info)).doesNotContain("sensitive-value-one", "sensitive-value-two",
				"sensitive-value-three", "endpoint-must-not-expose-this-token");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPolarisConfigEndpointExposesCompositeFileMetadata() {
		MockedConfigKVFile first = new MockedConfigKVFile(Map.of("first.key", "first-value"),
				"first.properties", testServiceName, testNamespace);
		MockedConfigKVFile second = new MockedConfigKVFile(Map.of("second.key", "second-value"),
				"second.properties", testServiceName, testNamespace);
		CompositeConfigFile composite = new CompositeConfigFile(List.of(first, second));
		PolarisPropertySource source = new PolarisPropertySource(testNamespace, testServiceName, "",
				composite, new HashMap<>());
		PolarisPropertySourceManager.addPropertySource(source);

		PolarisConfigEndpoint endpoint = new PolarisConfigEndpoint(polarisConfigProperties);
		List<Map<String, Object>> sources =
				(List<Map<String, Object>>) endpoint.polarisConfig().get("PolarisPropertySource");
		Map<String, Object> configKVFile = (Map<String, Object>) sources.get(0).get("configKVFile");
		List<Map<String, Object>> files =
				(List<Map<String, Object>>) configKVFile.get("configKVFiles");

		assertThat(files).extracting(file -> file.get("fileName"))
				.containsExactly("first.properties", "second.properties");
	}

	@Test
	public void testPolarisConfigEndpointExposesClientId() {
		SDKContext sdkContext = Mockito.mock(SDKContext.class);
		ValueContext valueContext = Mockito.mock(ValueContext.class);
		Mockito.when(sdkContext.getValueContext()).thenReturn(valueContext);
		Mockito.when(valueContext.getClientId()).thenReturn("host_1234_0");
		PolarisConfigSDKContextManager.setConfigSDKContext(sdkContext);

		PolarisConfigEndpoint endpoint = new PolarisConfigEndpoint(polarisConfigProperties);

		assertThat(endpoint.polarisConfig().get("ClientId")).isEqualTo("host_1234_0");
	}

	@Test
	public void testPolarisConfigEndpointClientIdIsNullWhenContextAbsent() {
		PolarisConfigEndpoint endpoint = new PolarisConfigEndpoint(polarisConfigProperties);

		assertThat(endpoint.polarisConfig().get("ClientId")).isNull();
	}
}
