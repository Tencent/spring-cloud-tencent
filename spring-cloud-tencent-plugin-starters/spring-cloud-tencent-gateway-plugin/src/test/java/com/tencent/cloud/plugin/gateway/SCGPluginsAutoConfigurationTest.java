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

package com.tencent.cloud.plugin.gateway;

import com.tencent.cloud.plugin.gateway.staining.TrafficStainingGatewayFilter;
import com.tencent.cloud.plugin.gateway.staining.rule.RuleStainingExecutor;
import com.tencent.cloud.plugin.gateway.staining.rule.RuleStainingProperties;
import com.tencent.cloud.plugin.gateway.staining.rule.RuleTrafficStainer;
import com.tencent.cloud.plugin.gateway.staining.rule.StainingRuleManager;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.factory.ConfigFileServiceFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

/**
 * Test for {@link SCGPluginsAutoConfiguration}.
 * @author derek.yi 2022-11-03
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = DEFINED_PORT, classes = SCGPluginsAutoConfigurationTest.TestApplication.class,
		properties = {"server.port=48081", "spring.config.location = classpath:application-test.yml",
				"spring.cloud.tencent.plugin.scg.staining.rule-staining.enabled = true"})
public class SCGPluginsAutoConfigurationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testAutoConfiguration() {
		assertThat(applicationContext.getBeansOfType(RuleStainingProperties.class).size()).isEqualTo(1);
		assertThat(applicationContext.getBeansOfType(StainingRuleManager.class).size()).isEqualTo(1);
		assertThat(applicationContext.getBeansOfType(TrafficStainingGatewayFilter.class).size()).isEqualTo(1);
		assertThat(applicationContext.getBeansOfType(RuleStainingExecutor.class).size()).isEqualTo(1);
		assertThat(applicationContext.getBeansOfType(RuleTrafficStainer.class).size()).isEqualTo(1);
	}

	@SpringBootApplication
	public static class TestApplication {

		@Bean
		public ConfigFileService configFileService(PolarisSDKContextManager polarisSDKContextManager) {
			return ConfigFileServiceFactory.createConfigFileService(polarisSDKContextManager.getSDKContext());
		}
	}
}
