/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.spring.property.PlaceholderHelper;
import com.tencent.cloud.polaris.config.spring.property.SpringValue;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import com.tencent.polaris.configuration.api.core.ChangeType;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import com.tencent.polaris.configuration.client.internal.CompositeConfigFile;
import com.tencent.polaris.configuration.client.internal.RevisableConfigFileGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

	@BeforeEach
	public void setUp() {
		PolarisPropertySourceManager.clearPropertySources();
	}

	@Test
	public void testConfigFileChanged() throws Exception {
		PolarisRefreshAffectedContextRefresher refresher = new PolarisRefreshAffectedContextRefresher(
				polarisConfigProperties, springValueRegistry, placeholderHelper, configFileService, contextRefresher);
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

		ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos);
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
				polarisConfigProperties, springValueRegistry, placeholderHelper, configFileService, contextRefresher);
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
		ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos);
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
}
