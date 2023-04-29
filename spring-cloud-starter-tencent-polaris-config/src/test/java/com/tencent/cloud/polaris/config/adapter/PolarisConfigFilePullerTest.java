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
package com.tencent.cloud.polaris.config.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.tencent.cloud.polaris.config.config.ConfigFileGroup;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.env.CompositePropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisConfigFilePuller}.
 *
 * @author wlx, youta
 */
@ExtendWith(MockitoExtension.class)
public class PolarisConfigFilePullerTest {

	private final String testNamespace = "testNamespace";
	private final String testServiceName = "testServiceName";
	private final String polarisConfigPropertySourceName = "polaris-config";
	@Mock
	private PolarisContextProperties polarisContextProperties;
	@Mock
	private ConfigFileService configFileService;
	@Mock
	private PolarisPropertySourceManager polarisPropertySourceManager;

	@Test
	public void testPullInternalConfigFiles() {
		PolarisConfigFilePuller puller = PolarisConfigFilePuller.get(polarisContextProperties, configFileService,
				polarisPropertySourceManager);

		when(polarisContextProperties.getNamespace()).thenReturn(testNamespace);
		when(polarisContextProperties.getService()).thenReturn(testServiceName);

		// application.properties
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("k1", "v1");
		applicationProperties.put("k2", "v2");
		applicationProperties.put("k3", "v3");
		ConfigKVFile propertiesFile = new MockedConfigKVFile(applicationProperties);
		when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "application.properties"))
				.thenReturn(propertiesFile);

		Map<String, Object> emptyMap = new HashMap<>();
		ConfigKVFile emptyConfigFile = new MockedConfigKVFile(emptyMap);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application.yml")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application.yaml")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "bootstrap.properties")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap.yml")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap.yaml")).thenReturn(emptyConfigFile);
		CompositePropertySource compositePropertySource = new CompositePropertySource(polarisConfigPropertySourceName);

		puller.initInternalConfigFiles(compositePropertySource, new String[] {}, new String[] {}, testServiceName);

		assertThat(compositePropertySource.getProperty("k1")).isEqualTo("v1");
		assertThat(compositePropertySource.getProperty("k2")).isEqualTo("v2");
		assertThat(compositePropertySource.getProperty("k3")).isEqualTo("v3");
	}

	@Test
	public void testPullInternalConfigFilesWithProfile() {
		PolarisConfigFilePuller puller = PolarisConfigFilePuller.get(polarisContextProperties, configFileService,
				polarisPropertySourceManager);

		when(polarisContextProperties.getNamespace()).thenReturn(testNamespace);
		when(polarisContextProperties.getService()).thenReturn(testServiceName);

		// application.properties
		Map<String, Object> applicationProperties = new HashMap<>();
		applicationProperties.put("k1", "v1");
		applicationProperties.put("k2", "v2");
		applicationProperties.put("k3", "v3");
		ConfigKVFile propertiesFile = new MockedConfigKVFile(applicationProperties);
		when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "application.properties"))
				.thenReturn(propertiesFile);

		// application-dev.properties
		Map<String, Object> devProperties = new HashMap<>();
		devProperties.put("k1", "v11");
		ConfigKVFile devFile = new MockedConfigKVFile(devProperties);
		when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "application-dev.properties"))
				.thenReturn(devFile);

		Map<String, Object> emptyMap = new HashMap<>();
		ConfigKVFile emptyConfigFile = new MockedConfigKVFile(emptyMap);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application.yml")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application.yaml")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application-dev.yml")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application-dev.yaml")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "bootstrap.properties")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "bootstrap-dev.properties")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap.yml")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap.yaml")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap-dev.yml")).thenReturn(emptyConfigFile);
		when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap-dev.yaml")).thenReturn(emptyConfigFile);
		List<String> active = new ArrayList<>();
		active.add("dev");
		String[] activeProfiles = active.toArray(new String[] {});
		CompositePropertySource compositePropertySource = new CompositePropertySource(polarisConfigPropertySourceName);
		puller.initInternalConfigFiles(compositePropertySource, activeProfiles, new String[] {}, testServiceName);

		assertThat(compositePropertySource.getProperty("k1")).isEqualTo("v11");
		assertThat(compositePropertySource.getProperty("k2")).isEqualTo("v2");
		assertThat(compositePropertySource.getProperty("k3")).isEqualTo("v3");
	}

	@Test
	public void testPullCustomConfigFilesWithProfile() {
		PolarisConfigFilePuller puller = PolarisConfigFilePuller.get(polarisContextProperties, configFileService,
				polarisPropertySourceManager);

		when(polarisContextProperties.getNamespace()).thenReturn(testNamespace);

		List<ConfigFileGroup> customFiles = new LinkedList<>();
		ConfigFileGroup configFileGroup = new ConfigFileGroup();
		String customGroup = "group1";
		configFileGroup.setName(customGroup);
		String customFile1 = "file1.properties";
		String customFile2 = "file2.properties";
		configFileGroup.setFiles(Lists.newArrayList(customFile1, customFile2));
		customFiles.add(configFileGroup);

		// file1.properties
		Map<String, Object> file1Map = new HashMap<>();
		file1Map.put("k1", "v1");
		file1Map.put("k2", "v2");
		ConfigKVFile file1 = new MockedConfigKVFile(file1Map);
		when(configFileService.getConfigPropertiesFile(testNamespace, customGroup, customFile1)).thenReturn(file1);

		// file2.properties
		Map<String, Object> file2Map = new HashMap<>();
		file2Map.put("k1", "v11");
		file2Map.put("k3", "v3");
		ConfigKVFile file2 = new MockedConfigKVFile(file2Map);
		when(configFileService.getConfigPropertiesFile(testNamespace, customGroup, customFile2)).thenReturn(file2);

		CompositePropertySource compositePropertySource = new CompositePropertySource(polarisConfigPropertySourceName);
		puller.initCustomPolarisConfigFiles(compositePropertySource, customFiles);

		assertThat(compositePropertySource.getProperty("k1")).isEqualTo("v1");
		assertThat(compositePropertySource.getProperty("k2")).isEqualTo("v2");
		assertThat(compositePropertySource.getProperty("k3")).isEqualTo("v3");
	}
}
