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

package com.tencent.cloud.polaris.config.adapter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.logger.PolarisConfigLoggerContext;
import com.tencent.cloud.polaris.config.spring.property.PlaceholderHelper;
import com.tencent.cloud.polaris.config.spring.property.SpringValue;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ChangeType;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import com.tencent.polaris.configuration.client.internal.CompositeConfigFile;
import com.tencent.polaris.configuration.client.internal.RevisableConfigFileGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * test for {@link PolarisRefreshAffectedContextRefresher}.
 *
 * @author lepdou 2022-06-11
 */
@ExtendWith(MockitoExtension.class)
public class PolarisPropertiesSourceAutoRefresherTest {

	private final String testNamespace = "testNamespace";
	private final String testFileGroup = "testFileGroup";
	private final String testServiceName = "testServiceName";
	private final String testFileName = "application.properties";
	@Mock
	private PolarisConfigProperties polarisConfigProperties;

	@Mock
	private SpringValueRegistry springValueRegistry;

	@Mock
	private PlaceholderHelper placeholderHelper;

	@Mock
	private ConfigFileService configFileService;

	@Mock
	private ContextRefresher contextRefresher;

	@Mock
	private SDKContext sdkContext;

	@Mock
	private Extensions extensions;

	@Mock
	private ValueContext valueContext;

	@BeforeEach
	public void setUp() {
		PolarisPropertySourceManager.clearPropertySources();
	}

	@Test
	public void testConfigFileChanged() throws Exception {
		PolarisRefreshAffectedContextRefresher refresher = new PolarisRefreshAffectedContextRefresher(
				polarisConfigProperties, springValueRegistry, placeholderHelper, configFileService,
				contextRefresher, sdkContext);
		ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
		ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
		TypeConverter typeConverter = mock(TypeConverter.class);
		when(beanFactory.getTypeConverter()).thenReturn(typeConverter);
		when(applicationContext.getBeanFactory()).thenReturn(beanFactory);
		refresher.setApplicationContext(applicationContext);
		when(typeConverter.convertIfNecessary(any(), any(), (Field) any())).thenReturn("v11");
		Collection<SpringValue> springValues = new ArrayList<>();
		MockedConfigChange mockedConfigChange = new MockedConfigChange();
		mockedConfigChange.setK1("v1");
		Field field = mockedConfigChange.getClass().getDeclaredField("k1");
		SpringValue springValue = new SpringValue("v1", "placeholder", mockedConfigChange, "mockedConfigChange", field);

		springValues.add(springValue);

		when(springValueRegistry.get(any(), any())).thenReturn(springValues);

		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);
		when(sdkContext.getExtensions()).thenReturn(extensions);
		when(extensions.getValueContext()).thenReturn(valueContext);
		when(valueContext.getClientId()).thenReturn("mockClientId");
		when(valueContext.getHost()).thenReturn("mockHost");

		Map<String, Object> content = new HashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v2");
		content.put("k3", "v3");
		MockedConfigKVFile file = new MockedConfigKVFile(content);
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, testServiceName, testFileName,
				file, content);

		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		ConfigPropertyChangeInfo changeInfo = new ConfigPropertyChangeInfo("k1", "v1", "v11", ChangeType.MODIFIED);
		ConfigPropertyChangeInfo changeInfo2 = new ConfigPropertyChangeInfo("k4", null, "v4", ChangeType.ADDED);
		ConfigPropertyChangeInfo changeInfo3 = new ConfigPropertyChangeInfo("k2", "v2", null, ChangeType.DELETED);
		ConfigPropertyChangeInfo changeInfoLogger = new ConfigPropertyChangeInfo("logging.level.root", null, "info", ChangeType.ADDED);

		Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
		changeInfos.put("k1", changeInfo);
		changeInfos.put("k2", changeInfo3);
		changeInfos.put("k4", changeInfo2);
		changeInfos.put("logging.level.root", changeInfoLogger);

		ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos, null);
		refresher.onApplicationEvent(null);

		file.fireChangeListener(event);

		assertThat(polarisPropertySource.getProperty("k1")).isEqualTo("v11");
		assertThat(polarisPropertySource.getProperty("k3")).isEqualTo("v3");
		assertThat(polarisPropertySource.getProperty("k2")).isNull();
		assertThat(polarisPropertySource.getProperty("k4")).isEqualTo("v4");
	}

	@Test
	public void testConfigFileGroupChanged() throws Exception {
		PolarisRefreshAffectedContextRefresher refresher = new PolarisRefreshAffectedContextRefresher(
				polarisConfigProperties, springValueRegistry, placeholderHelper, configFileService,
				contextRefresher, sdkContext);
		ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
		ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
		TypeConverter typeConverter = mock(TypeConverter.class);
		when(beanFactory.getTypeConverter()).thenReturn(typeConverter);
		when(applicationContext.getBeanFactory()).thenReturn(beanFactory);
		refresher.setApplicationContext(applicationContext);
		when(typeConverter.convertIfNecessary(any(), any(), (Field) any())).thenReturn("v11");
		Collection<SpringValue> springValues = new ArrayList<>();
		MockedConfigChange mockedConfigChange = new MockedConfigChange();
		mockedConfigChange.setK1("v1");
		Field field = mockedConfigChange.getClass().getDeclaredField("k1");
		SpringValue springValue = new SpringValue("v1", "placeholder", mockedConfigChange, "mockedConfigChange", field);

		springValues.add(springValue);

		when(springValueRegistry.get(any(), any())).thenReturn(springValues);

		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);

		Map<String, Object> content = new ConcurrentHashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v2");
		content.put("k3", "v3");
		MockedConfigKVFile file = new MockedConfigKVFile(content, testFileName, testFileGroup, testNamespace);
		when(configFileService.getConfigPropertiesFile(testNamespace, testFileGroup, testFileName))
				.thenReturn(file);

		CompositeConfigFile compositeConfigFile = new CompositeConfigFile(Collections.singletonList(file));
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, testFileGroup, testFileName,
				compositeConfigFile, new ConcurrentHashMap<>(content));

		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		RevisableConfigFileGroup revisableConfigFileGroup = new RevisableConfigFileGroup(testNamespace, testFileGroup, Collections.singletonList(file), "v1");
		when(configFileService.getConfigFileGroup(testNamespace, testFileGroup)).thenReturn(revisableConfigFileGroup);

		refresher.onApplicationEvent(null);

		Map<String, Object> content2 = new ConcurrentHashMap<>();
		content2.put("k1", "v1.1");
		content2.put("k3.1", "v3.1");
		MockedConfigKVFile file2 = new MockedConfigKVFile(content2, "file2.properties", testFileGroup, testNamespace);

		when(configFileService.getConfigPropertiesFile(testNamespace, testFileGroup, "file2.properties"))
				.thenReturn(file2);
		when(sdkContext.getExtensions()).thenReturn(extensions);
		when(extensions.getValueContext()).thenReturn(valueContext);

		revisableConfigFileGroup.updateConfigFileList(Arrays.asList(file, file2), "v2");
		Thread.sleep(5000);
		assertThat(polarisPropertySource.getProperty("k1")).isEqualTo("v1.1");
		assertThat(polarisPropertySource.getProperty("k2")).isEqualTo("v2");
		assertThat(polarisPropertySource.getProperty("k3")).isEqualTo("v3");
		assertThat(polarisPropertySource.getProperty("k3.1")).isEqualTo("v3.1");

		// delete event
		revisableConfigFileGroup.updateConfigFileList(Collections.singletonList(file), "v3");
		ConfigPropertyChangeInfo changeInfo = new ConfigPropertyChangeInfo("k1", "v1.1", null, ChangeType.DELETED);
		ConfigPropertyChangeInfo changeInfo2 = new ConfigPropertyChangeInfo("k3.1", "v3.1", null, ChangeType.DELETED);
		ConfigPropertyChangeInfo changeInfo3 = new ConfigPropertyChangeInfo("k4", null, "v4", ChangeType.ADDED);
		Map<String, ConfigPropertyChangeInfo> changeInfos = new TreeMap<>();
		changeInfos.put("k1", changeInfo);
		changeInfos.put("k3.1", changeInfo2);
		changeInfos.put("k4", changeInfo3);
		ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos, null);
		file2.fireChangeListener(event);
		Thread.sleep(5000);

		assertThat(polarisPropertySource.getProperty("k1")).isEqualTo("v1");
		assertThat(polarisPropertySource.getProperty("k3.1")).isNull();
		assertThat(polarisPropertySource.getProperty("k4")).isEqualTo("v4");

		revisableConfigFileGroup.updateConfigFileList(Arrays.asList(file, file2), "v4");
		Thread.sleep(5000);

		// no exception
		MockedConfigKVFile file3 = new MockedConfigKVFile(Collections.emptyMap(), "file3.properties", testFileGroup, testNamespace);
		revisableConfigFileGroup.updateConfigFileList(Arrays.asList(file, file2, file3), "v5");
		Thread.sleep(5000);

	}

	/**
	 * A change published between loading the property source and registering the listener fires no
	 * event to SCT, it must be recovered on {@code ApplicationReadyEvent}.
	 */
	@Test
	public void testReconcileConfigFileChangedBeforeListenerRegistered() throws Exception {
		ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
		PolarisRefreshAffectedContextRefresher refresher = newAffectedContextRefresher(applicationContext, "v11");
		MockedConfigChange mockedConfigChange = new MockedConfigChange();
		mockedConfigChange.setK1("v1");
		Field field = mockedConfigChange.getClass().getDeclaredField("k1");
		SpringValue springValue = new SpringValue("k1", "placeholder", mockedConfigChange, "mockedConfigChange", field);
		lenient().when(springValueRegistry.get(any(), eq("k1"))).thenReturn(Collections.singletonList(springValue));
		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);
		mockReportEvent();

		Map<String, Object> sdkContent = new HashMap<>();
		sdkContent.put("k1", "v1");
		sdkContent.put("k2", "v2");
		sdkContent.put("k3", "v3");
		MockedConfigKVFile file = new MockedConfigKVFile(sdkContent);
		// the environment holds a copy taken at loading time
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, testServiceName, testFileName,
				file, new ConcurrentHashMap<>(sdkContent));
		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		// published during startup: the SDK cache is updated, but no listener is registered yet
		sdkContent.put("k1", "v11");
		sdkContent.remove("k2");
		sdkContent.put("k4", "v4");

		refresher.onApplicationEvent(null);

		assertThat(polarisPropertySource.getProperty("k1")).isEqualTo("v11");
		assertThat(polarisPropertySource.getProperty("k2")).isNull();
		assertThat(polarisPropertySource.getProperty("k3")).isEqualTo("v3");
		assertThat(polarisPropertySource.getProperty("k4")).isEqualTo("v4");
		// @Value field
		assertThat(mockedConfigChange.getK1()).isEqualTo("v11");
		// @ConfigurationProperties beans
		ArgumentCaptor<EnvironmentChangeEvent> captor = ArgumentCaptor.forClass(EnvironmentChangeEvent.class);
		verify(applicationContext).publishEvent(captor.capture());
		assertThat(captor.getValue().getKeys()).containsExactlyInAnyOrder("k1", "k2", "k4");
		verify(contextRefresher, never()).refresh();
	}

	@Test
	public void testReconcileRefreshScopeKeyChangedBeforeListenerRegistered() {
		ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
		PolarisRefreshEntireContextRefresher refresher = new PolarisRefreshEntireContextRefresher(
				polarisConfigProperties, springValueRegistry, configFileService, contextRefresher, sdkContext);
		refresher.setApplicationContext(applicationContext);
		when(springValueRegistry.isRefreshScopeKey("k1")).thenReturn(true);
		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);
		mockReportEvent();

		Map<String, Object> sdkContent = new HashMap<>();
		sdkContent.put("k1", "v1");
		MockedConfigKVFile file = new MockedConfigKVFile(sdkContent);
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, testServiceName, testFileName,
				file, new ConcurrentHashMap<>(sdkContent));
		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		sdkContent.put("k1", "v11");

		refresher.onApplicationEvent(null);

		assertThat(polarisPropertySource.getProperty("k1")).isEqualTo("v11");
		// @RefreshScope beans
		verify(contextRefresher).refresh();
	}

	@Test
	public void testReconcileConfigFileGroupChangedBeforeListenerRegistered() throws Exception {
		String group = "reconcileFileGroup";
		ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
		PolarisRefreshAffectedContextRefresher refresher = newAffectedContextRefresher(applicationContext, "v1.1");
		when(springValueRegistry.get(any(), any())).thenReturn(Collections.emptyList());
		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);
		mockReportEvent();

		Map<String, Object> content = new ConcurrentHashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v2");
		MockedConfigKVFile file = new MockedConfigKVFile(content, testFileName, group, testNamespace);
		CompositeConfigFile compositeConfigFile = new CompositeConfigFile(Collections.singletonList(file));
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, group, "",
				compositeConfigFile, new ConcurrentHashMap<>(content));
		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		// published during startup: a key of a loaded file changed, and a new file joined the group
		content.put("k2", "v2.1");
		Map<String, Object> content2 = new ConcurrentHashMap<>();
		content2.put("k1", "v1.1");
		content2.put("k3", "v3");
		MockedConfigKVFile file2 = new MockedConfigKVFile(content2, "file2.properties", group, testNamespace);
		when(configFileService.getConfigPropertiesFile(testNamespace, group, testFileName)).thenReturn(file);
		when(configFileService.getConfigPropertiesFile(testNamespace, group, "file2.properties")).thenReturn(file2);
		RevisableConfigFileGroup revisableConfigFileGroup = new RevisableConfigFileGroup(testNamespace, group,
				Arrays.asList(file, file2), "v2");
		when(configFileService.getConfigFileGroup(testNamespace, group)).thenReturn(revisableConfigFileGroup);

		refresher.onApplicationEvent(null);

		assertThat(polarisPropertySource.getProperty("k2")).isEqualTo("v2.1");
		assertThat(polarisPropertySource.getProperty("k3")).isEqualTo("v3");
		// as a fresh start would load the group: the file listed first wins
		assertThat(polarisPropertySource.getProperty("k1")).isEqualTo("v1");

		// the new file is listened to from now on
		Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
		changeInfos.put("k3", new ConfigPropertyChangeInfo("k3", "v3", "v3.1", ChangeType.MODIFIED));
		file2.fireChangeListener(new ConfigKVFileChangeEvent(changeInfos, null));
		assertThat(polarisPropertySource.getProperty("k3")).isEqualTo("v3.1");
	}

	/**
	 * Several files added to a group during startup: the merged value follows the group order, as a
	 * fresh start would load it, not the iteration order of the added files.
	 */
	@Test
	public void testReconcileConfigFileGroupAddedFilesFollowGroupOrder() {
		String group = "reconcileOrderGroup";
		ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
		PolarisRefreshAffectedContextRefresher refresher = newAffectedContextRefresher(applicationContext, "any");
		when(springValueRegistry.get(any(), any())).thenReturn(Collections.emptyList());
		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);
		mockReportEvent();

		MockedConfigKVFile file = new MockedConfigKVFile(Collections.singletonMap("k0", "v0"), testFileName, group, testNamespace);
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, group, "",
				new CompositeConfigFile(Collections.singletonList(file)), new ConcurrentHashMap<>(Collections.singletonMap("k0", "v0")));
		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		// enough added files sharing the same key to make any hash-order dependency visible
		List<ConfigFileMetadata> groupFiles = new ArrayList<>();
		groupFiles.add(file);
		for (int i = 0; i < 20; i++) {
			String fileName = "added-" + i + ".properties";
			MockedConfigKVFile addedFile = new MockedConfigKVFile(Collections.singletonMap("shared", "from-" + i),
					fileName, group, testNamespace);
			lenient().when(configFileService.getConfigPropertiesFile(testNamespace, group, fileName)).thenReturn(addedFile);
			groupFiles.add(addedFile);
		}
		when(configFileService.getConfigFileGroup(testNamespace, group))
				.thenReturn(new RevisableConfigFileGroup(testNamespace, group, groupFiles, "v2"));

		refresher.onApplicationEvent(null);

		// "first file wins", as in CompositeConfigFile
		assertThat(polarisPropertySource.getProperty("shared")).isEqualTo("from-0");
		assertThat(polarisPropertySource.getProperty("k0")).isEqualTo("v0");
	}

	/**
	 * A file removed from a group during startup is deleted or unreleased, so its long polling clears
	 * its content in the SDK cache: the keys it contributed must leave the environment.
	 */
	@Test
	public void testReconcileConfigFileGroupRemovedFile() {
		String group = "reconcileRemovedGroup";
		ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
		PolarisRefreshAffectedContextRefresher refresher = newAffectedContextRefresher(applicationContext, "any");
		when(springValueRegistry.get(any(), any())).thenReturn(Collections.emptyList());
		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);
		mockReportEvent();

		Map<String, Object> content = new ConcurrentHashMap<>();
		content.put("k1", "v1");
		MockedConfigKVFile file = new MockedConfigKVFile(content, testFileName, group, testNamespace);
		Map<String, Object> content2 = new ConcurrentHashMap<>();
		content2.put("k1", "v1.2");
		content2.put("k2", "v2");
		MockedConfigKVFile file2 = new MockedConfigKVFile(content2, "file2.properties", group, testNamespace);
		Map<String, Object> loaded = new ConcurrentHashMap<>();
		loaded.put("k1", "v1");
		loaded.put("k2", "v2");
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, group, "",
				new CompositeConfigFile(Arrays.asList(file, file2)), loaded);
		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		// file2 removed during startup: the group drops it and its own long polling empties it
		content2.clear();
		when(configFileService.getConfigFileGroup(testNamespace, group)).thenReturn(
				new RevisableConfigFileGroup(testNamespace, group, Collections.singletonList(file), "v2"));

		refresher.onApplicationEvent(null);

		assertThat(polarisPropertySource.getProperty("k1")).isEqualTo("v1");
		assertThat(polarisPropertySource.getProperty("k2")).isNull();
		ArgumentCaptor<EnvironmentChangeEvent> captor = ArgumentCaptor.forClass(EnvironmentChangeEvent.class);
		verify(applicationContext).publishEvent(captor.capture());
		assertThat(captor.getValue().getKeys()).containsExactly("k2");
	}

	/**
	 * Same as a file added to a group after startup: logging.level keys of the added file take effect.
	 */
	@Test
	public void testReconcileConfigFileGroupAddedFileRefreshesLoggingLevel() {
		String group = "reconcileLoggingGroup";
		ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
		PolarisRefreshAffectedContextRefresher refresher = newAffectedContextRefresher(applicationContext, "any");
		when(springValueRegistry.get(any(), any())).thenReturn(Collections.emptyList());
		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);
		mockReportEvent();

		MockedConfigKVFile file = new MockedConfigKVFile(Collections.singletonMap("k0", "v0"), testFileName, group, testNamespace);
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, group, "",
				new CompositeConfigFile(Collections.singletonList(file)), new ConcurrentHashMap<>(Collections.singletonMap("k0", "v0")));
		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		MockedConfigKVFile file2 = new MockedConfigKVFile(Collections.singletonMap("logging.level.com.example", "debug"),
				"logging.properties", group, testNamespace);
		when(configFileService.getConfigPropertiesFile(testNamespace, group, "logging.properties")).thenReturn(file2);
		when(configFileService.getConfigFileGroup(testNamespace, group)).thenReturn(
				new RevisableConfigFileGroup(testNamespace, group, Arrays.asList(file, file2), "v2"));

		try (MockedStatic<PolarisConfigLoggerContext> loggerContext = mockStatic(PolarisConfigLoggerContext.class)) {
			refresher.onApplicationEvent(null);

			loggerContext.verify(() -> PolarisConfigLoggerContext.setLevel("com.example", "debug"));
		}
		assertThat(polarisPropertySource.getProperty("logging.level.com.example")).isEqualTo("debug");
	}

	@Test
	public void testReconcileNothingChangedBeforeListenerRegistered() {
		ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
		PolarisRefreshEntireContextRefresher refresher = new PolarisRefreshEntireContextRefresher(
				polarisConfigProperties, springValueRegistry, configFileService, contextRefresher, sdkContext);
		refresher.setApplicationContext(applicationContext);
		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);

		Map<String, Object> sdkContent = new HashMap<>();
		sdkContent.put("k1", "v1");
		MockedConfigKVFile file = new MockedConfigKVFile(sdkContent);
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, testServiceName, testFileName,
				file, new ConcurrentHashMap<>(sdkContent));
		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		refresher.onApplicationEvent(null);

		assertThat(polarisPropertySource.getProperty("k1")).isEqualTo("v1");
		verify(contextRefresher, never()).refresh();
		verify(applicationContext, never()).publishEvent(any());
	}

	private PolarisRefreshAffectedContextRefresher newAffectedContextRefresher(
			ConfigurableApplicationContext applicationContext, Object convertedValue) {
		PolarisRefreshAffectedContextRefresher refresher = new PolarisRefreshAffectedContextRefresher(
				polarisConfigProperties, springValueRegistry, placeholderHelper, configFileService,
				contextRefresher, sdkContext);
		ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
		TypeConverter typeConverter = mock(TypeConverter.class);
		when(beanFactory.getTypeConverter()).thenReturn(typeConverter);
		when(applicationContext.getBeanFactory()).thenReturn(beanFactory);
		lenient().when(typeConverter.convertIfNecessary(any(), any(), (Field) any())).thenReturn(convertedValue);
		refresher.setApplicationContext(applicationContext);
		return refresher;
	}

	private void mockReportEvent() {
		when(sdkContext.getExtensions()).thenReturn(extensions);
		when(extensions.getValueContext()).thenReturn(valueContext);
		lenient().when(valueContext.getClientId()).thenReturn("mockClientId");
		lenient().when(valueContext.getHost()).thenReturn("mockHost");
	}
}
