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

package com.tencent.cloud.polaris.circuitbreaker;

import com.tencent.cloud.polaris.circuitbreaker.config.PolarisRestTemplateAutoConfiguration;
import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisRestTemplateModifier;
import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisRestTemplateResponseErrorHandler;
import com.tencent.cloud.polaris.context.PolarisContextAutoConfiguration;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test For {@link PolarisRestTemplateAutoConfiguration} .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-06-28
 */
public class PolarisRestTemplateAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(
							PolarisRestTemplateAutoConfigurationTester.class,
							PolarisContextAutoConfiguration.class,
							PolarisRestTemplateAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.enabled=true");

	@Test
	public void testInitialization() {
		this.contextRunner
				.run(context -> {
					assertThat(context).hasSingleBean(PolarisRestTemplateModifier.class);
					assertThat(context).hasSingleBean(PolarisRestTemplateResponseErrorHandler.class);
				});
	}

	@Configuration
	@EnableAutoConfiguration
	@AutoConfigureBefore(PolarisRestTemplateAutoConfiguration.class)
	static class PolarisRestTemplateAutoConfigurationTester {

		@Bean
		RestTemplate restTemplate() {
			return new RestTemplate();
		}
	}

}
