package com.tencent.cloud.polaris.config.configdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.tencent.cloud.polaris.config.adapter.MockedConfigKVFile;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.factory.ConfigFileServiceFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisConfigDataLoader}.
 *
 * @author wlx
 * @date 2022/7/16 4:09 下午
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisConfigDataLoaderTest {

	@Mock
	private ConfigDataLoaderContext context;
	@Mock
	private PolarisConfigDataResource polarisConfigDataResource;
	@Mock
	private ConfigurableBootstrapContext bootstrapContext;
	@Mock
	private PolarisConfigProperties polarisConfigProperties;
	@Mock
	private PolarisContextProperties polarisContextProperties;
	@Mock
	private ConfigFileService configFileService;
	@Mock
	private Profiles profiles;

	private static SDKContext sdkContext;

	private final String testNamespace = "testNamespace";
	private final String testServiceName = "testServiceName";

	@Test
	public void loadConfigDataInternalConfigFilesTest() {
		try (MockedStatic<ConfigFileServiceFactory> mockedStatic = mockStatic(ConfigFileServiceFactory.class)) {
			Map<String, Object> emptyMap = new HashMap<>();
			ConfigKVFile emptyConfigFile = new MockedConfigKVFile(emptyMap);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application.yml")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "bootstrap.properties")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap.yml")).thenReturn(emptyConfigFile);
			Map<String, Object> applicationProperties = new HashMap<>();
			applicationProperties.put("k1", "v1");
			applicationProperties.put("k2", "v2");
			applicationProperties.put("k3", "v3");
			ConfigKVFile propertiesFile = new MockedConfigKVFile(applicationProperties);
			when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "application.properties"))
					.thenReturn(propertiesFile);

			mockSDKContext();
			when(context.getBootstrapContext()).thenReturn(bootstrapContext);
			when(bootstrapContext.isRegistered(eq(SDKContext.class))).thenReturn(false);
			when(bootstrapContext.get(eq(SDKContext.class))).thenReturn(sdkContext);

			when(bootstrapContext.isRegistered(eq(PolarisPropertySourceManager.class))).thenReturn(false);
			when(bootstrapContext.get(eq(PolarisPropertySourceManager.class))).thenReturn(new PolarisPropertySourceManager());

			when(polarisContextProperties.getNamespace()).thenReturn(testNamespace);
			when(polarisContextProperties.getService()).thenReturn(testServiceName);

			when(polarisConfigProperties.getGroups()).thenReturn(null);
			when(profiles.getActive()).thenReturn(Lists.newArrayList());

			PolarisConfigDataLoader polarisConfigDataLoader = new PolarisConfigDataLoader(new DeferredLogs());
			when(polarisConfigDataResource.getPolarisConfigProperties()).thenReturn(polarisConfigProperties);
			when(polarisConfigDataResource.getPolarisContextProperties()).thenReturn(polarisContextProperties);
			when(polarisConfigDataResource.getServiceName()).thenReturn(testServiceName);
			when(polarisConfigDataResource.getProfiles()).thenReturn(profiles);

			mockedStatic.when(() -> {
				ConfigFileServiceFactory.createConfigFileService(sdkContext);
			}).thenReturn(this.configFileService);

			ConfigData configData = polarisConfigDataLoader.load(context, polarisConfigDataResource);
			List<PropertySource<?>> propertySources = configData.getPropertySources();
			Optional<PropertySource<?>> propertySource = propertySources.stream().findFirst();
			if (propertySource.isPresent()) {
				PropertySource<?> source = propertySource.get();
				Assert.assertTrue(source instanceof CompositePropertySource);
				CompositePropertySource compositePropertySource = (CompositePropertySource) source;
				Assert.assertEquals("v1", compositePropertySource.getProperty("k1"));
				Assert.assertEquals("v2", compositePropertySource.getProperty("k2"));
				Assert.assertEquals("v3", compositePropertySource.getProperty("k3"));
			}
		}
	}

	@Test
	public void loadConfigDataInternalConfigFilesTestWithProfile() {
		try (MockedStatic<ConfigFileServiceFactory> mockedStatic = mockStatic(ConfigFileServiceFactory.class)) {
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
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application-dev.yml")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "bootstrap.properties")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "bootstrap-dev.properties")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap.yml")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap-dev.yml")).thenReturn(emptyConfigFile);

			when(polarisConfigProperties.getGroups()).thenReturn(null);
			when(polarisConfigProperties.getGroups()).thenReturn(null);
			List<String> active = new ArrayList<>();
			active.add("dev");
			when(profiles.getActive()).thenReturn(active);

			mockSDKContext();
			when(context.getBootstrapContext()).thenReturn(bootstrapContext);
			when(bootstrapContext.isRegistered(eq(SDKContext.class))).thenReturn(false);
			when(bootstrapContext.get(eq(SDKContext.class))).thenReturn(sdkContext);

			when(bootstrapContext.isRegistered(eq(PolarisPropertySourceManager.class))).thenReturn(false);
			when(bootstrapContext.get(eq(PolarisPropertySourceManager.class))).thenReturn(new PolarisPropertySourceManager());

			when(polarisContextProperties.getNamespace()).thenReturn(testNamespace);
			when(polarisContextProperties.getService()).thenReturn(testServiceName);

			when(polarisConfigProperties.getGroups()).thenReturn(null);

			PolarisConfigDataLoader polarisConfigDataLoader = new PolarisConfigDataLoader(new DeferredLogs());
			when(polarisConfigDataResource.getPolarisConfigProperties()).thenReturn(polarisConfigProperties);
			when(polarisConfigDataResource.getPolarisContextProperties()).thenReturn(polarisContextProperties);
			when(polarisConfigDataResource.getServiceName()).thenReturn(testServiceName);
			when(polarisConfigDataResource.getProfiles()).thenReturn(profiles);


			mockedStatic.when(() -> {
				ConfigFileServiceFactory.createConfigFileService(sdkContext);
			}).thenReturn(this.configFileService);

			ConfigData configData = polarisConfigDataLoader.load(context, polarisConfigDataResource);
			List<PropertySource<?>> propertySources = configData.getPropertySources();
			Optional<PropertySource<?>> propertySource = propertySources.stream().findFirst();
			if (propertySource.isPresent()) {
				PropertySource<?> source = propertySource.get();
				Assert.assertTrue(source instanceof CompositePropertySource);
				CompositePropertySource compositePropertySource = (CompositePropertySource) source;
				Assert.assertEquals("v11", compositePropertySource.getProperty("k1"));
				Assert.assertEquals("v2", compositePropertySource.getProperty("k2"));
				Assert.assertEquals("v3", compositePropertySource.getProperty("k3"));
			}
		}
	}

	@Test
	public void loadConfigDataCustomConfigFilesTestWithProfile() {
		try (MockedStatic<ConfigFileServiceFactory> mockedStatic = mockStatic(ConfigFileServiceFactory.class)) {
			Map<String, Object> emptyMap = new HashMap<>();
			ConfigKVFile emptyConfigFile = new MockedConfigKVFile(emptyMap);

			when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "application.properties")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "application.yml")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigPropertiesFile(testNamespace, testServiceName, "bootstrap.properties")).thenReturn(emptyConfigFile);
			when(configFileService.getConfigYamlFile(testNamespace, testServiceName, "bootstrap.yml")).thenReturn(emptyConfigFile);

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


			mockSDKContext();
			when(context.getBootstrapContext()).thenReturn(bootstrapContext);
			when(bootstrapContext.isRegistered(eq(SDKContext.class))).thenReturn(false);
			when(bootstrapContext.get(eq(SDKContext.class))).thenReturn(sdkContext);

			when(bootstrapContext.isRegistered(eq(PolarisPropertySourceManager.class))).thenReturn(false);
			when(bootstrapContext.get(eq(PolarisPropertySourceManager.class))).thenReturn(new PolarisPropertySourceManager());

			when(polarisContextProperties.getNamespace()).thenReturn(testNamespace);
			when(polarisContextProperties.getService()).thenReturn(testServiceName);

			when(polarisConfigProperties.getGroups()).thenReturn(null);
			when(profiles.getActive()).thenReturn(Lists.newArrayList());

			PolarisConfigDataLoader polarisConfigDataLoader = new PolarisConfigDataLoader(new DeferredLogs());
			when(polarisConfigDataResource.getPolarisConfigProperties()).thenReturn(polarisConfigProperties);
			when(polarisConfigDataResource.getPolarisContextProperties()).thenReturn(polarisContextProperties);
			when(polarisConfigDataResource.getServiceName()).thenReturn(testServiceName);
			when(polarisConfigDataResource.getProfiles()).thenReturn(profiles);

			mockedStatic.when(() -> {
				ConfigFileServiceFactory.createConfigFileService(sdkContext);
			}).thenReturn(this.configFileService);

			ConfigData configData = polarisConfigDataLoader.load(context, polarisConfigDataResource);
			List<PropertySource<?>> propertySources = configData.getPropertySources();
			Optional<PropertySource<?>> propertySource = propertySources.stream().findFirst();
			if (propertySource.isPresent()) {
				PropertySource<?> source = propertySource.get();
				Assert.assertTrue(source instanceof CompositePropertySource);
				CompositePropertySource compositePropertySource = (CompositePropertySource) source;
				Assert.assertEquals("v1", compositePropertySource.getProperty("k1"));
				Assert.assertEquals("v2", compositePropertySource.getProperty("k2"));
				Assert.assertEquals("v3", compositePropertySource.getProperty("k3"));
			}
		}
	}

	@AfterAll
	static void afterAll() {
		if (sdkContext != null) {
			sdkContext.destroy();
		}
	}

	private void mockSDKContext() {
		if (sdkContext == null) {
			sdkContext = SDKContext.initContext();
		}
	}
}
