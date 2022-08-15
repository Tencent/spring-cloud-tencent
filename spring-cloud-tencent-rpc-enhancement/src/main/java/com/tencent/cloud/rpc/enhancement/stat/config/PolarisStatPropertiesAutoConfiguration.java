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

package com.tencent.cloud.rpc.enhancement.stat.config;

import java.time.Duration;
import java.util.Map;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.rpc.enhancement.condition.ConditionalOnPushGatewayEnabled;
import com.tencent.cloud.rpc.enhancement.stat.config.plugin.PrometheusPushGatewayContainer;
import org.apache.commons.lang.StringUtils;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Autoconfiguration of stat reporter.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
@EnableConfigurationProperties(PolarisStatProperties.class)
public class PolarisStatPropertiesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public StatConfigModifier statReporterConfigModifier(PolarisStatProperties polarisStatProperties, Environment environment) {
		return new StatConfigModifier(polarisStatProperties, environment);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnPushGatewayEnabled
	public static class PolarisStatPrometheusPushGatewayAutoConfiguration {

		private static final String DEFAULT_JOB_NAME = "spring-cloud-tencent-application";

		@Bean
		@ConditionalOnMissingBean
		public PrometheusPushGatewayContainer prometheusPushGatewayContainer(PolarisStatProperties polarisStatProperties,
					Environment environment) {
			PolarisStatProperties.PushGatewayProperties pushGatewayProperties = polarisStatProperties.getPushgateway();
			String job = job(pushGatewayProperties, environment);
			Duration pushRate = pushGatewayProperties.getPushRate();
			String address = pushGatewayProperties.getAddress();
			PolarisStatProperties.ShutDownStrategy shutDownStrategy = pushGatewayProperties.getShutDownStrategy();
			Map<String, String> groupingKeys = pushGatewayProperties.getGroupingKeys();
			return new PrometheusPushGatewayContainer(address, pushRate, job, shutDownStrategy, groupingKeys);

		}

		private String job(PolarisStatProperties.PushGatewayProperties pushGatewayProperties, Environment environment) {
			String job = pushGatewayProperties.getJob();
			if (StringUtils.isBlank(job)) {
				job = environment.getProperty("spring.application.name");
			}
			if (StringUtils.isBlank(job)) {
				job = DEFAULT_JOB_NAME;
			}
			return job;
		}
	}
}
