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

package com.tencent.cloud.polaris.ratelimit.config;

import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.ratelimit.filter.QuotaCheckReactiveFilter;
import com.tencent.cloud.polaris.ratelimit.filter.QuotaCheckServletFilter;
import com.tencent.cloud.polaris.ratelimit.resolver.RateLimitRuleArgumentReactiveResolver;
import com.tencent.cloud.polaris.ratelimit.resolver.RateLimitRuleArgumentServletResolver;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisRateLimitAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
public class PolarisRateLimitAutoConfigurationTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner();

	private final WebApplicationContextRunner webApplicationContextRunner = new WebApplicationContextRunner();

	private final ReactiveWebApplicationContextRunner reactiveWebApplicationContextRunner = new ReactiveWebApplicationContextRunner();

	@Test
	public void testNoWebApplication() {
		this.applicationContextRunner
				.withConfiguration(AutoConfigurations.of(
						PolarisContextAutoConfiguration.class,
						PolarisRateLimitProperties.class,
						PolarisRateLimitAutoConfiguration.class))
				.run(context -> {
					assertThat(context).doesNotHaveBean(RateLimitRuleArgumentServletResolver.class);
					assertThat(context).doesNotHaveBean(RateLimitRuleArgumentReactiveResolver.class);
					assertThat(context).doesNotHaveBean(PolarisRateLimitAutoConfiguration.QuotaCheckFilterConfig.class);
					assertThat(context).doesNotHaveBean(QuotaCheckServletFilter.class);
					assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
					assertThat(context).doesNotHaveBean(PolarisRateLimitAutoConfiguration.MetadataReactiveFilterConfig.class);
					assertThat(context).doesNotHaveBean(QuotaCheckReactiveFilter.class);
				});
	}

	@Test
	public void testServletWebApplication() {
		this.webApplicationContextRunner
				.withConfiguration(AutoConfigurations.of(
						PolarisContextAutoConfiguration.class,
						PolarisRateLimitProperties.class,
						PolarisRateLimitAutoConfiguration.class))
				.run(context -> {
					assertThat(context).hasSingleBean(RateLimitRuleArgumentServletResolver.class);
					assertThat(context).hasSingleBean(PolarisRateLimitAutoConfiguration.QuotaCheckFilterConfig.class);
					assertThat(context).hasSingleBean(QuotaCheckServletFilter.class);
					assertThat(context).hasSingleBean(FilterRegistrationBean.class);
					assertThat(context).doesNotHaveBean(PolarisRateLimitAutoConfiguration.MetadataReactiveFilterConfig.class);
					assertThat(context).doesNotHaveBean(QuotaCheckReactiveFilter.class);
					assertThat(context).doesNotHaveBean(RateLimitRuleArgumentReactiveResolver.class);
				});
	}

	@Test
	public void testReactiveWebApplication() {
		this.reactiveWebApplicationContextRunner
				.withConfiguration(AutoConfigurations.of(
						PolarisContextAutoConfiguration.class,
						PolarisRateLimitProperties.class,
						PolarisRateLimitAutoConfiguration.class))
				.run(context -> {
					assertThat(context).doesNotHaveBean(RateLimitRuleArgumentServletResolver.class);
					assertThat(context).hasSingleBean(RateLimitRuleArgumentReactiveResolver.class);
					assertThat(context).doesNotHaveBean(PolarisRateLimitAutoConfiguration.QuotaCheckFilterConfig.class);
					assertThat(context).doesNotHaveBean(QuotaCheckServletFilter.class);
					assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
					assertThat(context).hasSingleBean(PolarisRateLimitAutoConfiguration.MetadataReactiveFilterConfig.class);
					assertThat(context).hasSingleBean(QuotaCheckReactiveFilter.class);
				});
	}
}
