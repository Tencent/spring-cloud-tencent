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

package com.tencent.cloud.polaris.ratelimit.config;

import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.ratelimit.filter.QuotaCheckReactiveFilter;
import com.tencent.cloud.polaris.ratelimit.filter.QuotaCheckServletFilter;
import com.tencent.cloud.polaris.ratelimit.resolver.RateLimitRuleArgumentReactiveResolver;
import com.tencent.cloud.polaris.ratelimit.resolver.RateLimitRuleArgumentServletResolver;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelReactiveResolver;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelServletResolver;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLimitedFallback;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import static com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant.FILTER_ORDER;
import static com.tencent.cloud.polaris.ratelimit.filter.QuotaCheckServletFilter.QUOTA_FILTER_BEAN_NAME;
import static jakarta.servlet.DispatcherType.ASYNC;
import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.FORWARD;
import static jakarta.servlet.DispatcherType.INCLUDE;
import static jakarta.servlet.DispatcherType.REQUEST;

/**
 * Configuration of rate limit.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(PolarisContextAutoConfiguration.class)
@ConditionalOnPolarisRateLimitEnabled
public class PolarisRateLimitAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public LimitAPI limitAPI(SDKContext polarisContext) {
		return LimitAPIFactory.createLimitAPIByContext(polarisContext);
	}

	/**
	 * Create when web application type is SERVLET.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	protected static class QuotaCheckFilterConfig {

		@Bean
		public RateLimitRuleArgumentServletResolver rateLimitRuleArgumentResolver(ServiceRuleManager serviceRuleManager,
				@Autowired(required = false) PolarisRateLimiterLabelServletResolver labelResolver) {
			return new RateLimitRuleArgumentServletResolver(serviceRuleManager, labelResolver);
		}

		@Bean
		@ConditionalOnMissingBean
		public QuotaCheckServletFilter quotaCheckFilter(LimitAPI limitAPI,
				PolarisRateLimitProperties polarisRateLimitProperties,
				RateLimitRuleArgumentServletResolver rateLimitRuleArgumentResolver,
				@Autowired(required = false) PolarisRateLimiterLimitedFallback polarisRateLimiterLimitedFallback) {
			return new QuotaCheckServletFilter(limitAPI, polarisRateLimitProperties, rateLimitRuleArgumentResolver, polarisRateLimiterLimitedFallback);
		}

		@Bean
		public FilterRegistrationBean<QuotaCheckServletFilter> quotaFilterRegistrationBean(
				QuotaCheckServletFilter quotaCheckServletFilter) {
			FilterRegistrationBean<QuotaCheckServletFilter> registrationBean = new FilterRegistrationBean<>(
					quotaCheckServletFilter);
			registrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE, REQUEST);
			registrationBean.setName(QUOTA_FILTER_BEAN_NAME);
			registrationBean.setOrder(FILTER_ORDER);
			return registrationBean;
		}


	}

	/**
	 * Create when web application type is REACTIVE.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	protected static class MetadataReactiveFilterConfig {

		@Bean
		public RateLimitRuleArgumentReactiveResolver rateLimitRuleArgumentResolver(ServiceRuleManager serviceRuleManager,
				@Autowired(required = false) PolarisRateLimiterLabelReactiveResolver labelResolver) {
			return new RateLimitRuleArgumentReactiveResolver(serviceRuleManager, labelResolver);
		}

		@Bean
		public QuotaCheckReactiveFilter quotaCheckReactiveFilter(LimitAPI limitAPI,
				PolarisRateLimitProperties polarisRateLimitProperties,
				RateLimitRuleArgumentReactiveResolver rateLimitRuleArgumentResolver,
				@Nullable PolarisRateLimiterLimitedFallback polarisRateLimiterLimitedFallback) {
			return new QuotaCheckReactiveFilter(limitAPI, polarisRateLimitProperties, rateLimitRuleArgumentResolver, polarisRateLimiterLimitedFallback);
		}
	}
}
