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

import java.util.List;
import java.util.Map;

import com.tencent.cloud.metadata.core.filter.gateway.Metadata2HeaderScgFilter;
import com.tencent.cloud.metadata.core.interceptor.Metadata2HeaderFeignInterceptor;
import com.tencent.cloud.metadata.core.interceptor.Metadata2HeaderRestTemplateInterceptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Metadata transfer auto configuration.
 *
 * @author Haotian Zhang
 */
@Configuration
public class MetadataTransferAutoConfiguration {

	/**
	 * Create when gateway application is SCG.
	 */
	@Configuration
	@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
	static class MetadataTransferScgFilterConfig {

		@Bean
		public GlobalFilter metadata2HeaderScgFilter() {
			return new Metadata2HeaderScgFilter();
		}

	}

	/**
	 * Create when Feign exists.
	 */
	@Configuration
	@ConditionalOnClass(name = "feign.Feign")
	static class MetadataTransferFeignInterceptorConfig {

		@Bean
		public Metadata2HeaderFeignInterceptor metadata2HeaderFeignInterceptor() {
			return new Metadata2HeaderFeignInterceptor();
		}

	}

	/**
	 * Create when RestTemplate exists.
	 */
	@Configuration
	@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
	static class MetadataTransferRestTemplateConfig implements ApplicationContextAware {

		private ApplicationContext context;

		@Bean
		public Metadata2HeaderRestTemplateInterceptor metadata2HeaderRestTemplateInterceptor() {
			return new Metadata2HeaderRestTemplateInterceptor();
		}

		@Bean
		BeanPostProcessor metadata2HeaderRestTemplatePostProcessor(
				Metadata2HeaderRestTemplateInterceptor metadata2HeaderRestTemplateInterceptor) {
			// Coping with multiple bean injection scenarios
			Map<String, RestTemplate> beans = this.context.getBeansOfType(RestTemplate.class);
			// If the restTemplate has been created when the
			// MetadataRestTemplatePostProcessor Bean
			// is initialized, then manually set the interceptor.
			if (!CollectionUtils.isEmpty(beans)) {
				for (RestTemplate restTemplate : beans.values()) {
					List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
					// Avoid setting interceptor repeatedly.
					if (null != interceptors && !interceptors.contains(metadata2HeaderRestTemplateInterceptor)) {
						interceptors.add(metadata2HeaderRestTemplateInterceptor);
						restTemplate.setInterceptors(interceptors);
					}
				}
			}
			return new Metadata2HeaderRestTemplatePostProcessor(metadata2HeaderRestTemplateInterceptor);
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.context = applicationContext;
		}

		public static class Metadata2HeaderRestTemplatePostProcessor implements BeanPostProcessor {

			private Metadata2HeaderRestTemplateInterceptor metadata2HeaderRestTemplateInterceptor;

			Metadata2HeaderRestTemplatePostProcessor(
					Metadata2HeaderRestTemplateInterceptor metadata2HeaderRestTemplateInterceptor) {
				this.metadata2HeaderRestTemplateInterceptor = metadata2HeaderRestTemplateInterceptor;
			}

			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return bean;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof RestTemplate) {
					RestTemplate restTemplate = (RestTemplate) bean;
					List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
					// Avoid setting interceptor repeatedly.
					if (null != interceptors && !interceptors.contains(metadata2HeaderRestTemplateInterceptor)) {
						interceptors.add(this.metadata2HeaderRestTemplateInterceptor);
						restTemplate.setInterceptors(interceptors);
					}
				}
				return bean;
			}

		}

	}

}
