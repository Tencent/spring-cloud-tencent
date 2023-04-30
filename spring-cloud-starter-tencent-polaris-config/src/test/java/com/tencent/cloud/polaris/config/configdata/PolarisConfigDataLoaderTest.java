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

package com.tencent.cloud.polaris.config.configdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.tencent.cloud.polaris.config.adapter.MockedConfigKVFile;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.factory.ConfigFileServiceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;

import static com.tencent.cloud.polaris.config.configdata.PolarisConfigDataLoader.CUSTOM_POLARIS_CONFIG_FILE_LOADED;
import static com.tencent.cloud.polaris.config.configdata.PolarisConfigDataLoader.INTERNAL_CONFIG_FILES_LOADED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisConfigDataLoader}.
 *
 * @author wlx
 */
@ExtendWith(MockitoExtension.class)
public class PolarisConfigDataLoaderTest {

	private static final SDKContext sdkContext = SDKContext.initContext();

	private final String testNamespace = "testNamespace";
	private final String testServiceName = "testServiceName";
	private final String polarisConfigPropertySourceName = "polaris-config";

	@AfterAll
	static void afterAll() {
		if (sdkContext != null) {
			sdkContext.destroy();
		}
	}

	@Test
	public void loadConfigDataInternalConfigFilesTest() {
		try (MockedStatic<ConfigFileServiceFactory> mockedStatic = mockStatic(ConfigFileServiceFactory.class)) {
			ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class);
			PolarisConfigDataResource polarisConfigDataResource = mock(PolarisConfigDataResource.class);
			ConfigurableBootstrapContext bootstrapContext = mock(ConfigurableBootstrapContext.class);
			PolarisConfigProperties polarisConfigProperties = mock(PolarisConfigProperties.class);
			PolarisContextProperties polarisContextProperties = mock(PolarisContextProperties.class);
			ConfigFileService configFileService = mock(ConfigFileService.class);
			Profiles profiles = mock(Profiles.class);
			Map<String, Object> emptyMap = new HashMap<>();
			ConfigKVFile emptyConfigFile = new MockedConfigKVFile(emptyMap);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application.yml")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application.yaml")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "bootstrap.properties")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap.yml")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap.yaml")).thenReturn(emptyConfigFile);
			Map<String, Object> applicationProperties = new HashMap<>();
			applicationProperties.put("k1", "v1");
			applicationProperties.put("k2", "v2");
			applicationProperties.put("k3", "v3");
			ConfigKVFile propertiesFile = new MockedConfigKVFile(applicationProperties);
			when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "application.properties"))
					.thenReturn(propertiesFile);
			when(context.getBootstrapContext()).thenReturn(bootstrapContext);
			when(bootstrapContext.get(eq(SDKContext.class))).thenReturn(sdkContext);

			when(bootstrapContext.get(eq(PolarisPropertySourceManager.class))).thenReturn(new PolarisPropertySourceManager());

			when(polarisContextProperties.getNamespace()).thenReturn(testNamespace);
			when(polarisContextProperties.getService()).thenReturn(testServiceName);

			when(polarisConfigProperties.getGroups()).thenReturn(null);
			when(profiles.getActive()).thenReturn(Lists.newArrayList());

			PolarisConfigDataLoader polarisConfigDataLoader = new PolarisConfigDataLoader(new DeferredLogs());
			if (INTERNAL_CONFIG_FILES_LOADED.get()) {
				INTERNAL_CONFIG_FILES_LOADED.compareAndSet(true, false);
			}
			if (CUSTOM_POLARIS_CONFIG_FILE_LOADED.get()) {
				CUSTOM_POLARIS_CONFIG_FILE_LOADED.compareAndSet(true, false);
			}
			when(polarisConfigDataResource.getPolarisConfigProperties()).thenReturn(polarisConfigProperties);
			when(polarisConfigDataResource.getPolarisContextProperties()).thenReturn(polarisContextProperties);
			when(polarisConfigDataResource.getServiceName()).thenReturn(testServiceName);
			when(polarisConfigDataResource.getProfiles()).thenReturn(profiles);

			mockedStatic.when(() -> {
				ConfigFileServiceFactory.createConfigFileService(sdkContext);
			}).thenReturn(configFileService);

			ConfigData configData = polarisConfigDataLoader.load(context, polarisConfigDataResource);
			List<PropertySource<?>> propertySources = configData.getPropertySources();
			CompositePropertySource compositePropertySource = new CompositePropertySource(polarisConfigPropertySourceName);
			propertySources.forEach(compositePropertySource::addPropertySource);
			assertThat(compositePropertySource.getProperty("k1")).isEqualTo("v1");
			assertThat(compositePropertySource.getProperty("k2")).isEqualTo("v2");
			assertThat(compositePropertySource.getProperty("k3")).isEqualTo("v3");
		}
	}

	@Test
	public void loadConfigDataInternalConfigFilesTestWithProfile() {
		try (MockedStatic<ConfigFileServiceFactory> mockedStatic = mockStatic(ConfigFileServiceFactory.class)) {
			ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class);
			PolarisConfigDataResource polarisConfigDataResource = mock(PolarisConfigDataResource.class);
			ConfigurableBootstrapContext bootstrapContext = mock(ConfigurableBootstrapContext.class);
			PolarisConfigProperties polarisConfigProperties = mock(PolarisConfigProperties.class);
			PolarisContextProperties polarisContextProperties = mock(PolarisContextProperties.class);
			ConfigFileService configFileService = mock(ConfigFileService.class);
			Profiles profiles = mock(Profiles.class);
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

			when(polarisConfigProperties.getGroups()).thenReturn(null);
			when(polarisConfigProperties.getGroups()).thenReturn(null);
			List<String> active = new ArrayList<>();
			active.add("dev");
			when(profiles.getActive()).thenReturn(active);

			when(context.getBootstrapContext()).thenReturn(bootstrapContext);
			when(bootstrapContext.get(eq(SDKContext.class))).thenReturn(sdkContext);
			when(bootstrapContext.get(eq(PolarisPropertySourceManager.class))).thenReturn(new PolarisPropertySourceManager());

			when(polarisContextProperties.getNamespace()).thenReturn(testNamespace);
			when(polarisContextProperties.getService()).thenReturn(testServiceName);

			when(polarisConfigProperties.getGroups()).thenReturn(null);

			PolarisConfigDataLoader polarisConfigDataLoader = new PolarisConfigDataLoader(new DeferredLogs());
			if (INTERNAL_CONFIG_FILES_LOADED.get()) {
				INTERNAL_CONFIG_FILES_LOADED.compareAndSet(true, false);
			}
			if (CUSTOM_POLARIS_CONFIG_FILE_LOADED.get()) {
				CUSTOM_POLARIS_CONFIG_FILE_LOADED.compareAndSet(true, false);
			}
			when(polarisConfigDataResource.getPolarisConfigProperties()).thenReturn(polarisConfigProperties);
			when(polarisConfigDataResource.getPolarisContextProperties()).thenReturn(polarisContextProperties);
			when(polarisConfigDataResource.getServiceName()).thenReturn(testServiceName);
			when(polarisConfigDataResource.getProfiles()).thenReturn(profiles);

			mockedStatic.when(() -> {
				ConfigFileServiceFactory.createConfigFileService(sdkContext);
			}).thenReturn(configFileService);

			ConfigData configData = polarisConfigDataLoader.load(context, polarisConfigDataResource);
			List<PropertySource<?>> propertySources = configData.getPropertySources();

			CompositePropertySource compositePropertySource = new CompositePropertySource(polarisConfigPropertySourceName);
			propertySources.forEach(compositePropertySource::addPropertySource);

			assertThat(compositePropertySource.getProperty("k1")).isEqualTo("v11");
			assertThat(compositePropertySource.getProperty("k2")).isEqualTo("v2");
			assertThat(compositePropertySource.getProperty("k3")).isEqualTo("v3");
		}
	}

	@Test
	public void loadConfigDataCustomConfigFilesTestWithProfile() {
		try (MockedStatic<ConfigFileServiceFactory> mockedStatic = mockStatic(ConfigFileServiceFactory.class)) {
			ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class);
			PolarisConfigDataResource polarisConfigDataResource = mock(PolarisConfigDataResource.class);
			ConfigurableBootstrapContext bootstrapContext = mock(ConfigurableBootstrapContext.class);
			PolarisConfigProperties polarisConfigProperties = mock(PolarisConfigProperties.class);
			PolarisContextProperties polarisContextProperties = mock(PolarisContextProperties.class);
			ConfigFileService configFileService = mock(ConfigFileService.class);
			Profiles profiles = mock(Profiles.class);
			Map<String, Object> emptyMap = new HashMap<>();
			ConfigKVFile emptyConfigFile = new MockedConfigKVFile(emptyMap);

			when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "application.properties")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application.yml")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application.yaml")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "bootstrap.properties")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap.yml")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap.yaml")).thenReturn(emptyConfigFile);

			String customGroup = "group1";
			String customFile1 = "file1.properties";
			when(polarisConfigDataResource.getFileName()).thenReturn(customFile1);
			when(polarisConfigDataResource.getGroupName()).thenReturn(customGroup);

			when(polarisConfigProperties.getGroups()).thenReturn(null);
			when(profiles.getActive()).thenReturn(Lists.newArrayList());

			// file1.properties
			Map<String, Object> file1Map = new HashMap<>();
			file1Map.put("k1", "v1");
			file1Map.put("k2", "v2");
			file1Map.put("k3", "v3");
			ConfigKVFile file1 = new MockedConfigKVFile(file1Map);
			when(configFileService.getConfigPropertiesFile(testNamespace, customGroup, customFile1)).thenReturn(file1);

			when(context.getBootstrapContext()).thenReturn(bootstrapContext);
			when(bootstrapContext.get(eq(SDKContext.class))).thenReturn(sdkContext);

			when(bootstrapContext.get(eq(PolarisPropertySourceManager.class))).thenReturn(new PolarisPropertySourceManager());

			when(polarisContextProperties.getNamespace()).thenReturn(testNamespace);
			when(polarisContextProperties.getService()).thenReturn(testServiceName);

			when(polarisConfigProperties.getGroups()).thenReturn(null);
			when(profiles.getActive()).thenReturn(Lists.newArrayList());

			PolarisConfigDataLoader polarisConfigDataLoader = new PolarisConfigDataLoader(new DeferredLogs());

			if (INTERNAL_CONFIG_FILES_LOADED.get()) {
				INTERNAL_CONFIG_FILES_LOADED.compareAndSet(true, false);
			}
			if (CUSTOM_POLARIS_CONFIG_FILE_LOADED.get()) {
				CUSTOM_POLARIS_CONFIG_FILE_LOADED.compareAndSet(true, false);
			}
			when(polarisConfigDataResource.getPolarisConfigProperties()).thenReturn(polarisConfigProperties);
			when(polarisConfigDataResource.getPolarisContextProperties()).thenReturn(polarisContextProperties);
			when(polarisConfigDataResource.getServiceName()).thenReturn(testServiceName);
			when(polarisConfigDataResource.getProfiles()).thenReturn(profiles);

			mockedStatic.when(() -> {
				ConfigFileServiceFactory.createConfigFileService(sdkContext);
			}).thenReturn(configFileService);

			ConfigData configData = polarisConfigDataLoader.load(context, polarisConfigDataResource);
			List<PropertySource<?>> propertySources = configData.getPropertySources();
			CompositePropertySource compositePropertySource = new CompositePropertySource(polarisConfigPropertySourceName);
			propertySources.forEach(compositePropertySource::addPropertySource);

			assertThat(compositePropertySource.getProperty("k1")).isEqualTo("v1");
			assertThat(compositePropertySource.getProperty("k2")).isEqualTo("v2");
			assertThat(compositePropertySource.getProperty("k3")).isEqualTo("v3");
		}
	}
}
