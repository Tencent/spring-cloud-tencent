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

package com.tencent.cloud.polaris.extend.consul;

import java.util.List;
import java.util.Map;

import com.tencent.cloud.polaris.DiscoveryPropertiesAutoConfiguration;
import com.tencent.cloud.polaris.context.PolarisContextAutoConfiguration;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.INSTANCE_ID_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.IP_ADDRESS_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.PREFER_IP_ADDRESS_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.SERVICE_NAME_KEY;
import static com.tencent.polaris.test.common.Consts.HOST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConsulContextProperties}.
 *
 * @author Haotian Zhang
 */
public class ConsulContextPropertiesTest {

	@Test
	public void testModify() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner().withConfiguration(
						AutoConfigurations.of(PolarisContextAutoConfiguration.class,
								ConsulContextPropertiesTest.TestConfiguration.class,
								DiscoveryPropertiesAutoConfiguration.class))
				.withPropertyValues("spring.cloud.consul.discovery.register=true")
				.withPropertyValues("spring.cloud.consul.discovery.enabled=true")
				.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
				.withPropertyValues("spring.cloud.consul.discovery.instance-id=ins-test")
				.withPropertyValues("spring.cloud.consul.discovery.prefer-ip-address=true")
				.withPropertyValues("spring.cloud.consul.discovery.ip-address=" + HOST);
		applicationContextRunner.run(context -> {
			assertThat(context).hasSingleBean(SDKContext.class);
			SDKContext sdkContext = context.getBean(SDKContext.class);
			com.tencent.polaris.api.config.Configuration configuration = sdkContext.getConfig();
			List<ServerConnectorConfigImpl> serverConnectorConfigs = configuration.getGlobal().getServerConnectors();
			Map<String, String> metadata = null;
			for (ServerConnectorConfigImpl serverConnectorConfig : serverConnectorConfigs) {
				if (serverConnectorConfig.getId().equals("consul")) {
					metadata = serverConnectorConfig.getMetadata();
				}
			}
			assertThat(metadata).isNotNull();
			assertThat(metadata.get(SERVICE_NAME_KEY)).isEqualTo(SERVICE_PROVIDER);
			assertThat(metadata.get(INSTANCE_ID_KEY)).isEqualTo("ins-test");
			assertThat(metadata.get(PREFER_IP_ADDRESS_KEY)).isEqualTo("true");
			assertThat(metadata.get(IP_ADDRESS_KEY)).isEqualTo(HOST);
		});
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public ConsulContextProperties consulContextProperties() {
			ConsulContextProperties consulContextProperties = new ConsulContextProperties();
			consulContextProperties.setEnabled(true);
			return consulContextProperties;
		}
	}
}
