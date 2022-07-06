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

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.ratelimit.RateLimitRuleLabelResolver;
import com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant;
import com.tencent.cloud.polaris.ratelimit.filter.QuotaCheckReactiveFilter;
import com.tencent.cloud.polaris.ratelimit.filter.QuotaCheckServletFilter;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelReactiveResolver;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelServletResolver;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import static com.tencent.cloud.polaris.ratelimit.filter.QuotaCheckServletFilter.QUOTA_FILTER_BEAN_NAME;
import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;

/**
 * Configuration of rate limit.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
@AutoConfigureAfter(PolarisContextAutoConfiguration.class)
@ConditionalOnProperty(name = "spring.cloud.polaris.ratelimit.enabled", matchIfMissing = true)
public class PolarisRateLimitAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public LimitAPI limitAPI(SDKContext polarisContext) {
		return LimitAPIFactory.createLimitAPIByContext(polarisContext);
	}

	@Bean
	public RateLimitRuleLabelResolver rateLimitRuleLabelService(ServiceRuleManager serviceRuleManager) {
		return new RateLimitRuleLabelResolver(serviceRuleManager);
	}

	/**
	 * Create when web application type is SERVLET.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	protected static class QuotaCheckFilterConfig {

		@Bean
		@ConditionalOnMissingBean
		public QuotaCheckServletFilter quotaCheckFilter(LimitAPI limitAPI,
				@Nullable PolarisRateLimiterLabelServletResolver labelResolver,
				PolarisRateLimitProperties polarisRateLimitProperties,
				RateLimitRuleLabelResolver rateLimitRuleLabelResolver) {
			return new QuotaCheckServletFilter(limitAPI, labelResolver,
					polarisRateLimitProperties, rateLimitRuleLabelResolver);
		}

		@Bean
		public FilterRegistrationBean<QuotaCheckServletFilter> quotaFilterRegistrationBean(
				QuotaCheckServletFilter quotaCheckServletFilter) {
			FilterRegistrationBean<QuotaCheckServletFilter> registrationBean = new FilterRegistrationBean<>(
					quotaCheckServletFilter);
			registrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE, REQUEST);
			registrationBean.setName(QUOTA_FILTER_BEAN_NAME);
			registrationBean.setOrder(RateLimitConstant.FILTER_ORDER);
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
		public QuotaCheckReactiveFilter quotaCheckReactiveFilter(LimitAPI limitAPI,
				@Nullable PolarisRateLimiterLabelReactiveResolver labelResolver,
				PolarisRateLimitProperties polarisRateLimitProperties,
				RateLimitRuleLabelResolver rateLimitRuleLabelResolver) {
			return new QuotaCheckReactiveFilter(limitAPI, labelResolver,
					polarisRateLimitProperties, rateLimitRuleLabelResolver);
		}
	}
}
