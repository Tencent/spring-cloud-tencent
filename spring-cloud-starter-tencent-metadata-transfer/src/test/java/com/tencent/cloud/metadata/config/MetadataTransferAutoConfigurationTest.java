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

package com.tencent.cloud.metadata.config;

import com.tencent.cloud.metadata.core.EncodeTransferMedataFeignEnhancedPlugin;
import com.tencent.cloud.metadata.core.EncodeTransferMedataRestTemplateEnhancedPlugin;
import com.tencent.cloud.metadata.core.EncodeTransferMedataWebClientEnhancedPlugin;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link MetadataTransferAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
public class MetadataTransferAutoConfigurationTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner();
	private final ReactiveWebApplicationContextRunner reactiveWebApplicationContextRunner = new ReactiveWebApplicationContextRunner();

	/**
	 * No any web application.
	 */
	@Test
	public void test1() {
		this.applicationContextRunner.withConfiguration(AutoConfigurations.of(MetadataTransferAutoConfiguration.class))
				.run(context -> {
					assertThat(context).hasSingleBean(MetadataTransferAutoConfiguration.MetadataTransferFeignInterceptorConfig.class);
					assertThat(context).hasSingleBean(EncodeTransferMedataFeignEnhancedPlugin.class);
					assertThat(context).hasSingleBean(EncodeTransferMedataRestTemplateEnhancedPlugin.class);
					assertThat(context).hasSingleBean(MetadataTransferAutoConfiguration.MetadataTransferRestTemplateConfig.class);
					assertThat(context).hasSingleBean(MetadataTransferAutoConfiguration.MetadataTransferScgFilterConfig.class);
				});
	}

	/**
	 * Reactive web application.
	 */
	@Test
	public void test2() {
		this.reactiveWebApplicationContextRunner.withConfiguration(AutoConfigurations.of(MetadataTransferAutoConfiguration.class, PolarisContextProperties.class))
				.run(context -> {
					assertThat(context).hasSingleBean(MetadataTransferAutoConfiguration.MetadataTransferFeignInterceptorConfig.class);
					assertThat(context).hasSingleBean(EncodeTransferMedataFeignEnhancedPlugin.class);
					assertThat(context).hasSingleBean(MetadataTransferAutoConfiguration.MetadataTransferRestTemplateConfig.class);
					assertThat(context).hasSingleBean(EncodeTransferMedataRestTemplateEnhancedPlugin.class);
					assertThat(context).hasSingleBean(MetadataTransferAutoConfiguration.MetadataTransferScgFilterConfig.class);
					assertThat(context).hasSingleBean(EncodeTransferMedataWebClientEnhancedPlugin.class);
				});
	}

	@Configuration
	static class RestTemplateConfiguration {

		@Bean
		public RestTemplate restTemplate() {
			return new RestTemplate();
		}

		@LoadBalanced
		@Bean
		public RestTemplate loadBalancedRestTemplate() {
			return new RestTemplate();
		}
	}
}
