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

package com.tencent.cloud.common.metadata.config;

import com.netflix.zuul.ZuulFilter;
import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.aop.MetadataFeignAspect;
import com.tencent.cloud.common.metadata.filter.gateway.MetadataFirstScgFilter;
import com.tencent.cloud.common.metadata.filter.gateway.MetadataFirstZuulFilter;
import com.tencent.cloud.common.metadata.filter.web.MetadataReactiveFilter;
import com.tencent.cloud.common.metadata.filter.web.MetadataServletFilter;
import com.tencent.cloud.common.metadata.interceptor.feign.MetadataFirstFeignInterceptor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;

/**
 * Metadata auto configuration.
 *
 * @author Haotian Zhang
 */
@Configuration
public class MetadataAutoConfiguration {

	/**
	 * metadata properties.
	 * @return metadata properties
	 */
	@Bean
	public MetadataLocalProperties metadataLocalProperties() {
		return new MetadataLocalProperties();
	}

	/**
	 * Create when web application type is SERVLET.
	 */
	@Configuration
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	static class MetadataServletFilterConfig {

		@Bean
		public FilterRegistrationBean<MetadataServletFilter> metadataServletFilterRegistrationBean(
				MetadataServletFilter metadataServletFilter) {
			FilterRegistrationBean<MetadataServletFilter> filterRegistrationBean = new FilterRegistrationBean<>(
					metadataServletFilter);
			filterRegistrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE,
					REQUEST);
			filterRegistrationBean
					.setOrder(MetadataConstant.OrderConstant.WEB_FILTER_ORDER);
			return filterRegistrationBean;
		}

		@Bean
		public MetadataServletFilter metadataServletFilter() {
			return new MetadataServletFilter();
		}

	}

	/**
	 * Create when web application type is REACTIVE.
	 */
	@Configuration
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	static class MetadataReactiveFilterConfig {

		@Bean
		public MetadataReactiveFilter metadataReactiveFilter() {
			return new MetadataReactiveFilter();
		}

	}

	/**
	 * Create when Feign exists.
	 */
	@Configuration
	@ConditionalOnClass(name = "feign.Feign")
	static class MetadataFeignInterceptorConfig {

		@Bean
		public MetadataFirstFeignInterceptor metadataFirstFeignInterceptor() {
			return new MetadataFirstFeignInterceptor();
		}

		@Bean
		public MetadataFeignAspect metadataFeignAspect() {
			return new MetadataFeignAspect();
		}

	}

	/**
	 * Create when gateway application is Zuul.
	 */
	@Configuration
	@ConditionalOnClass(name = "com.netflix.zuul.http.ZuulServlet")
	static class MetadataZuulFilterConfig {

		@Bean
		public ZuulFilter metadataFirstZuulFilter() {
			return new MetadataFirstZuulFilter();
		}

	}

	/**
	 * Create when gateway application is SCG.
	 */
	@Configuration
	@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
	static class MetadataScgFilterConfig {

		@Bean
		public GlobalFilter metadataFirstScgFilter() {
			return new MetadataFirstScgFilter();
		}

	}

}
