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

package com.tencent.cloud.polaris.config.listener;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Lists;
import com.tencent.cloud.polaris.config.adapter.MockedConfigKVFile;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySource;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.adapter.PolarisRefreshAffectedContextRefresher;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.enums.RefreshType;
import com.tencent.polaris.configuration.api.core.ChangeType;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.tencent.cloud.polaris.config.condition.ReflectRefreshTypeCondition.POLARIS_CONFIG_REFRESH_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

/**
 * test for {@link PolarisConfigRefreshOptimizationListener}.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = DEFINED_PORT, classes = PolarisConfigRefreshOptimizationListenerNotTriggeredTest.TestApplication.class,
		properties = {
				"server.port=48081",
				"spring.cloud.polaris.address=grpc://127.0.0.1:10081",
				"spring.cloud.polaris.config.connect-remote-server=false",
				"spring.cloud.polaris.config.refresh-type=reflect",
				"spring.config.location = classpath:application-test.yml"
		})
public class PolarisConfigRefreshOptimizationListenerNotTriggeredTest {

	private static final String REFLECT_REFRESHER_BEAN_NAME = "polarisReflectPropertySourceAutoRefresher";

	private static final String TEST_NAMESPACE = "testNamespace";

	private static final String TEST_SERVICE_NAME = "testServiceName";

	private static final String TEST_FILE_NAME = "application.properties";

	@Autowired
	private ConfigurableApplicationContext context;

	@Test
	public void testNotSwitchConfigRefreshType() {
		RefreshType actualRefreshType = context.getEnvironment()
				.getProperty(POLARIS_CONFIG_REFRESH_TYPE, RefreshType.class);
		assertThat(actualRefreshType).isEqualTo(RefreshType.REFLECT);
		PolarisConfigProperties polarisConfigProperties = context.getBean(PolarisConfigProperties.class);
		assertThat(polarisConfigProperties.getRefreshType()).isEqualTo(RefreshType.REFLECT);
		assertThat(context.containsBean(REFLECT_REFRESHER_BEAN_NAME)).isTrue();
		PolarisRefreshAffectedContextRefresher refresher = context
				.getBean(REFLECT_REFRESHER_BEAN_NAME, PolarisRefreshAffectedContextRefresher.class);
		assertThat(((AbstractApplicationContext) context).getApplicationListeners().contains(refresher)).isTrue();
	}

	@Test
	public void testConfigFileChanged() {
		Map<String, Object> content = new HashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v2");
		content.put("k3", "v3");
		MockedConfigKVFile file = new MockedConfigKVFile(content);

		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(TEST_NAMESPACE, TEST_SERVICE_NAME, TEST_FILE_NAME,
				file, content);
		PolarisPropertySourceManager manager = context.getBean(PolarisPropertySourceManager.class);
		when(manager.getAllPropertySources()).thenReturn(Lists.newArrayList(polarisPropertySource));

		PolarisRefreshAffectedContextRefresher refresher = context.getBean(PolarisRefreshAffectedContextRefresher.class);
		PolarisRefreshAffectedContextRefresher spyRefresher = Mockito.spy(refresher);

		spyRefresher.onApplicationEvent(null);

		ConfigPropertyChangeInfo changeInfo = new ConfigPropertyChangeInfo("k1", "v1", "v11", ChangeType.MODIFIED);
		ConfigPropertyChangeInfo changeInfo2 = new ConfigPropertyChangeInfo("k4", null, "v4", ChangeType.ADDED);
		ConfigPropertyChangeInfo changeInfo3 = new ConfigPropertyChangeInfo("k2", "v2", null, ChangeType.DELETED);
		Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
		changeInfos.put("k1", changeInfo);
		changeInfos.put("k2", changeInfo3);
		changeInfos.put("k4", changeInfo2);
		ConfigKVFileChangeEvent event = new ConfigKVFileChangeEvent(changeInfos);
		file.fireChangeListener(event);

		ContextRefresher mockContextRefresher = context.getBean(ContextRefresher.class);
		when(mockContextRefresher.refresh()).thenReturn(event.changedKeys());

		Mockito.verify(spyRefresher, Mockito.times(1))
				.refreshSpringValue("k1");
		Mockito.verify(spyRefresher, Mockito.times(1))
				.refreshSpringValue("k2");
		Mockito.verify(spyRefresher, Mockito.times(1))
				.refreshSpringValue("k4");
		Mockito.verify(spyRefresher, Mockito.times(1))
				.refreshConfigurationProperties(event.changedKeys());
	}

	@SpringBootApplication
	protected static class TestApplication {

		@Primary
		@Bean
		public PolarisPropertySourceManager polarisPropertySourceManager() {
			return mock(PolarisPropertySourceManager.class);
		}

		@Primary
		@Bean
		public ContextRefresher contextRefresher() {
			return mock(ContextRefresher.class);
		}

		@Component
		protected static class TestBeanWithoutRefreshScope {

		}
	}
}
