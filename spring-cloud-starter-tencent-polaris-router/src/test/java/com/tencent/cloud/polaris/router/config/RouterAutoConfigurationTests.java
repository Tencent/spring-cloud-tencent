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

package com.tencent.cloud.polaris.router.config;

import com.tencent.cloud.common.metadata.config.MetadataAutoConfiguration;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * test for {@link RouterAutoConfiguration }.
 * @author dongyinuo
 */
public class RouterAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					TestRestTemplatesConfiguration.class,
					MetadataAutoConfiguration.class,
					RouterAutoConfiguration.class,
					RouterBootstrapAutoConfiguration.class,
					PolarisContextAutoConfiguration.class,
					RouterAutoConfiguration.RouterLabelRestTemplateConfig.class,
					ApplicationContextAwareUtils.class
			)).withPropertyValues("spring.application.name=test");

	@Test
	public void testRouterLabelRestTemplateConfig() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(RouterAutoConfiguration.RouterLabelRestTemplateConfig.class);
		});
	}

	@Configuration
	static class TestRestTemplatesConfiguration {
		@Bean
		public RestTemplate restTemplate() {
			return new RestTemplate();
		}
	}

}
