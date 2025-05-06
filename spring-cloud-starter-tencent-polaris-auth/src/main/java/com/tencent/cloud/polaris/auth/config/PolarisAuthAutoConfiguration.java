/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.auth.config;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.auth.filter.AuthReactiveFilter;
import com.tencent.cloud.polaris.auth.filter.AuthServletFilter;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.cloud.polaris.auth.filter.AuthServletFilter.AUTH_FILTER_BEAN_NAME;
import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;

/**
 * Auto configuration for auth.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(PolarisContextAutoConfiguration.class)
public class PolarisAuthAutoConfiguration {

	/**
	 * Create when web application type is SERVLET.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	protected static class AuthServletFilterConfig {

		@Bean
		@ConditionalOnMissingBean
		public AuthServletFilter authServletFilter(PolarisSDKContextManager polarisSDKContextManager) {
			return new AuthServletFilter(polarisSDKContextManager.getAuthAPI());
		}

		@Bean
		public FilterRegistrationBean<AuthServletFilter> authFilterRegistrationBean(
				AuthServletFilter authServletFilter) {
			FilterRegistrationBean<AuthServletFilter> registrationBean = new FilterRegistrationBean<>(
					authServletFilter);
			registrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE, REQUEST);
			registrationBean.setName(AUTH_FILTER_BEAN_NAME);
			registrationBean.setOrder(OrderConstant.Server.Servlet.AUTH_FILTER_ORDER);
			return registrationBean;
		}
	}

	/**
	 * Create when web application type is REACTIVE.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	protected static class AuthReactiveFilterConfig {

		@Bean
		public AuthReactiveFilter authReactiveFilter(PolarisSDKContextManager polarisSDKContextManager) {
			return new AuthReactiveFilter(polarisSDKContextManager.getAuthAPI());
		}
	}
}
