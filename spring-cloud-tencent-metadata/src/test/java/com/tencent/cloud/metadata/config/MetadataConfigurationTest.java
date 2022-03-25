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

package com.tencent.cloud.metadata.config;

import com.tencent.cloud.metadata.core.filter.web.MetadataReactiveFilter;
import com.tencent.cloud.metadata.core.filter.web.MetadataServletFilter;
import com.tencent.cloud.metadata.core.interceptor.feign.Metadata2HeaderFeignInterceptor;
import com.tencent.cloud.metadata.core.interceptor.feign.MetadataFirstFeignInterceptor;
import com.tencent.cloud.metadata.core.interceptor.resttemplate.MetadataRestTemplateInterceptor;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test for {@link MetadataConfiguration}
 *
 * @author Haotian Zhang
 */
public class MetadataConfigurationTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner();

	private final WebApplicationContextRunner webApplicationContextRunner = new WebApplicationContextRunner();

	private final ReactiveWebApplicationContextRunner reactiveWebApplicationContextRunner = new ReactiveWebApplicationContextRunner();

	/**
	 * No any web application.
	 */
	@Test
	public void test1() {
		this.applicationContextRunner
				.withConfiguration(AutoConfigurations.of(MetadataConfiguration.class))
				.run(context -> {
					Assertions.assertThat(context)
							.hasSingleBean(MetadataLocalProperties.class);
					Assertions.assertThat(context).doesNotHaveBean(
							MetadataConfiguration.MetadataReactiveFilterConfig.class);
					Assertions.assertThat(context)
							.doesNotHaveBean(MetadataReactiveFilter.class);
					Assertions.assertThat(context).doesNotHaveBean(
							MetadataConfiguration.MetadataServletFilterConfig.class);
					Assertions.assertThat(context)
							.doesNotHaveBean(MetadataServletFilter.class);
					// Assertions.assertThat(context)
					// .doesNotHaveBean(MetadataConfiguration.MetadataZuulFilterConfig.class);
					// Assertions.assertThat(context)
					// .doesNotHaveBean(MetadataFirstZuulFilter.class);
					// Assertions.assertThat(context)
					// .doesNotHaveBean(Metadata2HeaderZuulFilter.class);
					// Assertions.assertThat(context)
					// .doesNotHaveBean(MetadataConfiguration.MetadataScgFilterConfig.class);
					// Assertions.assertThat(context)
					// .doesNotHaveBean(MetadataFirstScgFilter.class);
					// Assertions.assertThat(context)
					// .doesNotHaveBean(Metadata2HeaderScgFilter.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataConfiguration.MetadataFeignPluginConfig.class);
					Assertions.assertThat(context)
							.hasSingleBean(MetadataFirstFeignInterceptor.class);
					Assertions.assertThat(context)
							.hasSingleBean(Metadata2HeaderFeignInterceptor.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataConfiguration.MetadataRestTemplateConfig.class);
					Assertions.assertThat(context)
							.hasSingleBean(MetadataRestTemplateInterceptor.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataConfiguration.MetadataRestTemplateConfig.MetadataRestTemplatePostProcessor.class);
				});
	}

	/**
	 * web application.
	 */
	@Test
	public void test2() {
		this.webApplicationContextRunner
				.withConfiguration(AutoConfigurations.of(MetadataConfiguration.class))
				.run(context -> {
					Assertions.assertThat(context)
							.hasSingleBean(MetadataLocalProperties.class);
					Assertions.assertThat(context).doesNotHaveBean(
							MetadataConfiguration.MetadataReactiveFilterConfig.class);
					Assertions.assertThat(context)
							.doesNotHaveBean(MetadataReactiveFilter.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataConfiguration.MetadataServletFilterConfig.class);
					Assertions.assertThat(context)
							.hasSingleBean(MetadataServletFilter.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataConfiguration.MetadataFeignPluginConfig.class);
					Assertions.assertThat(context)
							.hasSingleBean(MetadataFirstFeignInterceptor.class);
					Assertions.assertThat(context)
							.hasSingleBean(Metadata2HeaderFeignInterceptor.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataConfiguration.MetadataRestTemplateConfig.class);
					Assertions.assertThat(context)
							.hasSingleBean(MetadataRestTemplateInterceptor.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataConfiguration.MetadataRestTemplateConfig.MetadataRestTemplatePostProcessor.class);
				});
	}

	/**
	 * reactive web application.
	 */
	@Test
	public void test3() {
		this.reactiveWebApplicationContextRunner
				.withConfiguration(AutoConfigurations.of(MetadataConfiguration.class))
				.run(context -> {
					Assertions.assertThat(context)
							.hasSingleBean(MetadataLocalProperties.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataConfiguration.MetadataReactiveFilterConfig.class);
					Assertions.assertThat(context)
							.hasSingleBean(MetadataReactiveFilter.class);
					Assertions.assertThat(context).doesNotHaveBean(
							MetadataConfiguration.MetadataServletFilterConfig.class);
					Assertions.assertThat(context)
							.doesNotHaveBean(MetadataServletFilter.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataConfiguration.MetadataFeignPluginConfig.class);
					Assertions.assertThat(context)
							.hasSingleBean(MetadataFirstFeignInterceptor.class);
					Assertions.assertThat(context)
							.hasSingleBean(Metadata2HeaderFeignInterceptor.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataConfiguration.MetadataRestTemplateConfig.class);
					Assertions.assertThat(context)
							.hasSingleBean(MetadataRestTemplateInterceptor.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataConfiguration.MetadataRestTemplateConfig.MetadataRestTemplatePostProcessor.class);
				});
	}

}
