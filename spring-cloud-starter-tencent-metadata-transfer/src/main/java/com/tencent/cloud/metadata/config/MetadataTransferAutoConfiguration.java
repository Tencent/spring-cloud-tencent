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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.metadata.core.DecodeTransferMetadataReactiveFilter;
import com.tencent.cloud.metadata.core.DecodeTransferMetadataServletFilter;
import com.tencent.cloud.metadata.core.EncodeTransferMedataFeignInterceptor;
import com.tencent.cloud.metadata.core.EncodeTransferMedataRestTemplateInterceptor;
import com.tencent.cloud.metadata.core.EncodeTransferMedataScgFilter;
import com.tencent.cloud.metadata.core.EncodeTransferMedataWebClientFilter;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;

/**
 * Metadata transfer auto configuration.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
public class MetadataTransferAutoConfiguration {

	/**
	 * Create when web application type is SERVLET.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	protected static class MetadataServletFilterConfig {

		@Bean
		public FilterRegistrationBean<DecodeTransferMetadataServletFilter> metadataServletFilterRegistrationBean(
				DecodeTransferMetadataServletFilter decodeTransferMetadataServletFilter) {
			FilterRegistrationBean<DecodeTransferMetadataServletFilter> filterRegistrationBean =
					new FilterRegistrationBean<>(decodeTransferMetadataServletFilter);
			filterRegistrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE, REQUEST);
			filterRegistrationBean.setOrder(OrderConstant.Server.Servlet.DECODE_TRANSFER_METADATA_FILTER_ORDER);
			return filterRegistrationBean;
		}

		@Bean
		public DecodeTransferMetadataServletFilter metadataServletFilter() {
			return new DecodeTransferMetadataServletFilter();
		}
	}

	/**
	 * Create when web application type is REACTIVE.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	protected static class MetadataReactiveFilterConfig {

		@Bean
		public DecodeTransferMetadataReactiveFilter metadataReactiveFilter() {
			return new DecodeTransferMetadataReactiveFilter();
		}
	}

	/**
	 * Create when gateway application is SCG.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
	protected static class MetadataTransferScgFilterConfig {

		@Bean
		public GlobalFilter encodeTransferMedataScgFilter() {
			return new EncodeTransferMedataScgFilter();
		}
	}

	/**
	 * Create when Feign exists.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "feign.Feign")
	protected static class MetadataTransferFeignInterceptorConfig {

		@Bean
		public EncodeTransferMedataFeignInterceptor encodeTransferMedataFeignInterceptor() {
			return new EncodeTransferMedataFeignInterceptor();
		}
	}

	/**
	 * Create when RestTemplate exists.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
	protected static class MetadataTransferRestTemplateConfig {

		@Autowired(required = false)
		private List<RestTemplate> restTemplates = Collections.emptyList();

		@Bean
		public EncodeTransferMedataRestTemplateInterceptor encodeTransferMedataRestTemplateInterceptor() {
			return new EncodeTransferMedataRestTemplateInterceptor();
		}

		@Bean
		public SmartInitializingSingleton addEncodeTransferMetadataInterceptorForRestTemplate(EncodeTransferMedataRestTemplateInterceptor interceptor) {
			return () -> restTemplates.forEach(restTemplate -> {
				List<ClientHttpRequestInterceptor> list = new ArrayList<>(restTemplate.getInterceptors());
				list.add(interceptor);
				restTemplate.setInterceptors(list);
			});
		}
	}

	/**
	 * Create when WebClient.Builder exists.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
	protected static class MetadataTransferWebClientConfig {
		@Autowired(required = false)
		private List<WebClient.Builder> webClientBuilder = Collections.emptyList();

		@Bean
		public EncodeTransferMedataWebClientFilter encodeTransferMedataWebClientFilter() {
			return new EncodeTransferMedataWebClientFilter();
		}

		@Bean
		public SmartInitializingSingleton addEncodeTransferMetadataFilterForWebClient(EncodeTransferMedataWebClientFilter filter) {
			return () -> webClientBuilder.forEach(webClient -> {
				webClient.filter(filter);
			});
		}
	}
}
