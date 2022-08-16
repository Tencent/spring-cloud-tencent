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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.tencent.cloud.metadata.core.EncodeTransferMedataFeignInterceptor;
import com.tencent.cloud.metadata.core.EncodeTransferMedataRestTemplateInterceptor;
import com.tencent.cloud.metadata.core.EncodeTransferMetadataZuulFilter;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

/**
 * Test for {@link MetadataTransferAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
public class MetadataTransferAutoConfigurationTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner();

	/**
	 * No any web application.
	 */
	@Test
	public void test1() {
		this.applicationContextRunner
				.withConfiguration(
						AutoConfigurations.of(MetadataTransferAutoConfiguration.class))
				.run(context -> {
					Assertions.assertThat(context).hasSingleBean(
							MetadataTransferAutoConfiguration.MetadataTransferFeignInterceptorConfig.class);
					Assertions.assertThat(context)
							.hasSingleBean(EncodeTransferMedataFeignInterceptor.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataTransferAutoConfiguration.MetadataTransferRestTemplateConfig.class);
					Assertions.assertThat(context).hasSingleBean(
							EncodeTransferMedataRestTemplateInterceptor.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataTransferAutoConfiguration.MetadataTransferZuulFilterConfig.class);
					Assertions.assertThat(context)
							.hasSingleBean(EncodeTransferMetadataZuulFilter.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataTransferAutoConfiguration.MetadataTransferScgFilterConfig.class);
					Assertions.assertThat(context).hasSingleBean(GlobalFilter.class);
				});
	}


	@Test
	public void test2() {
		this.applicationContextRunner
				.withConfiguration(
						AutoConfigurations.of(MetadataTransferAutoConfiguration.class, RestTemplateConfiguration.class))
				.run(context -> {
					Assertions.assertThat(context)
							.hasSingleBean(EncodeTransferMedataFeignInterceptor.class);
					EncodeTransferMedataRestTemplateInterceptor encodeTransferMedataRestTemplateInterceptor = context.getBean(EncodeTransferMedataRestTemplateInterceptor.class);
					Map<String, RestTemplate> restTemplateMap = context.getBeansOfType(RestTemplate.class);
					Assertions.assertThat(restTemplateMap.size()).isEqualTo(2);
					for (String beanName : Arrays.asList("restTemplate", "loadBalancedRestTemplate")) {
						RestTemplate restTemplate = restTemplateMap.get(beanName);
						Assertions.assertThat(restTemplate).isNotNull();
						List<ClientHttpRequestInterceptor> encodeTransferMedataFeignInterceptorList = restTemplate.getInterceptors()
								.stream()
								.filter(interceptor -> Objects.equals(interceptor, encodeTransferMedataRestTemplateInterceptor))
								.collect(Collectors.toList());
						//EncodeTransferMetadataFeignInterceptor is not added repeatedly
						Assertions.assertThat(encodeTransferMedataFeignInterceptorList.size()).isEqualTo(1);
					}
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
