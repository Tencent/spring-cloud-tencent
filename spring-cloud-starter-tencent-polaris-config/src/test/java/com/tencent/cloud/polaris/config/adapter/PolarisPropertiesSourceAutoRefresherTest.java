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

package com.tencent.cloud.polaris.config.adapter;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Lists;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.polaris.configuration.api.core.ChangeType;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.context.refresh.ContextRefresher;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * test for {@link PolarisPropertySourceAutoRefresher}.
 *@author lepdou 2022-06-11
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisPropertiesSourceAutoRefresherTest {

	private final String testNamespace = "testNamespace";
	private final String testServiceName = "testServiceName";
	private final String testFileName = "application.properties";
	@Mock
	private PolarisConfigProperties polarisConfigProperties;
	@Mock
	private PolarisPropertySourceManager polarisPropertySourceManager;
	@Mock
	private ContextRefresher contextRefresher;

	@Test
	public void testConfigFileChanged() {
		PolarisPropertySourceAutoRefresher refresher = new PolarisPropertySourceAutoRefresher(polarisConfigProperties,
				polarisPropertySourceManager, contextRefresher);

		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);

		Map<String, Object> content = new HashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v2");
		content.put("k3", "v3");
		MockedConfigKVFile file = new MockedConfigKVFile(content);
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, testServiceName, testFileName,
				file, content);

		when(polarisPropertySourceManager.getAllPropertySources()).thenReturn(Lists.newArrayList(polarisPropertySource));

		ConfigPropertyChangeInfo changeInfo = new ConfigPropertyChangeInfo("k1", "v1", "v11", ChangeType.MODIFIED);
		ConfigPropertyChangeInfo changeInfo2 = new ConfigPropertyChangeInfo("k4", null, "v4", ChangeType.ADDED);
		ConfigPropertyChangeInfo changeInfo3 = new ConfigPropertyChangeInfo("k2", "v2", null, ChangeType.DELETED);
		Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
		changeInfos.put("k1", changeInfo);
		changeInfos.put("k2", changeInfo3);
		changeInfos.put("k4", changeInfo2);

		ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos);
		refresher.onApplicationEvent(null);

		file.fireChangeListener(event);

		Assert.assertEquals("v11", polarisPropertySource.getProperty("k1"));
		Assert.assertEquals("v3", polarisPropertySource.getProperty("k3"));
		Assert.assertNull(polarisPropertySource.getProperty("k2"));
		Assert.assertEquals("v4", polarisPropertySource.getProperty("k4"));
		verify(contextRefresher).refresh();
	}

	@Test
	public void testNewConfigFile() {
		PolarisPropertySourceAutoRefresher refresher = new PolarisPropertySourceAutoRefresher(polarisConfigProperties,
				polarisPropertySourceManager, contextRefresher);

		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);

		Map<String, Object> emptyContent = new HashMap<>();
		MockedConfigKVFile file = new MockedConfigKVFile(emptyContent);
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, testServiceName, testFileName,
				file, emptyContent);

		when(polarisPropertySourceManager.getAllPropertySources()).thenReturn(Lists.newArrayList(polarisPropertySource));

		ConfigPropertyChangeInfo changeInfo = new ConfigPropertyChangeInfo("k1", null, "v1", ChangeType.ADDED);
		Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
		changeInfos.put("k1", changeInfo);

		ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos);
		refresher.onApplicationEvent(null);

		file.fireChangeListener(event);

		Assert.assertEquals("v1", polarisPropertySource.getProperty("k1"));
		verify(contextRefresher).refresh();
	}
}
