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

import com.netflix.zuul.ZuulFilter;
import com.tencent.cloud.metadata.constant.MetadataConstant;
import com.tencent.cloud.metadata.core.filter.gateway.scg.Metadata2HeaderScgFilter;
import com.tencent.cloud.metadata.core.filter.gateway.scg.MetadataFirstScgFilter;
import com.tencent.cloud.metadata.core.filter.gateway.zuul.Metadata2HeaderZuulFilter;
import com.tencent.cloud.metadata.core.filter.gateway.zuul.MetadataFirstZuulFilter;
import com.tencent.cloud.metadata.core.filter.web.MetadataReactiveFilter;
import com.tencent.cloud.metadata.core.filter.web.MetadataServletFilter;
import com.tencent.cloud.metadata.core.interceptor.feign.Metadata2HeaderFeignInterceptor;
import com.tencent.cloud.metadata.core.interceptor.feign.MetadataFirstFeignInterceptor;
import com.tencent.cloud.metadata.core.interceptor.resttemplate.MetadataRestTemplateInterceptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;

/**
 * Metadata Configuration.
 *
 * @author Haotian Zhang
 */
@Configuration
public class MetadataConfiguration {

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
			filterRegistrationBean.setOrder(MetadataConstant.OrderConstant.FILTER_ORDER);
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
	 * Create when gateway application is Zuul.
	 */
	@Configuration
	@ConditionalOnClass(name = "com.netflix.zuul.http.ZuulServlet")
	static class MetadataZuulFilterConfig {

		@Bean
		public ZuulFilter metadataFirstZuulFilter() {
			return new MetadataFirstZuulFilter();
		}

		@Bean
		public ZuulFilter metadata2HeaderZuulFilter() {
			return new Metadata2HeaderZuulFilter();
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
	static class MetadataFeignPluginConfig {

		@Bean
		public MetadataFirstFeignInterceptor metadataFirstFeignInterceptor() {
			return new MetadataFirstFeignInterceptor();
		}

		@Bean
		public Metadata2HeaderFeignInterceptor metadataFeignInterceptor() {
			return new Metadata2HeaderFeignInterceptor();
		}

	}

	/**
	 * Create when RestTemplate exists.
	 */
	@Configuration
	@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
	static class MetadataRestTemplateConfig implements ApplicationContextAware {

		private ApplicationContext context;

		@Bean
		public MetadataRestTemplateInterceptor metadataRestTemplateInterceptor() {
			return new MetadataRestTemplateInterceptor();
		}

		@Bean
		BeanPostProcessor metadataRestTemplatePostProcessor(
				MetadataRestTemplateInterceptor metadataRestTemplateInterceptor) {
			// Coping with multiple bean injection scenarios
			Map<String, RestTemplate> beans = this.context
					.getBeansOfType(RestTemplate.class);
			// If the restTemplate has been created when the
			// MetadataRestTemplatePostProcessor Bean
			// is initialized, then manually set the interceptor.
			if (!CollectionUtils.isEmpty(beans)) {
				for (RestTemplate restTemplate : beans.values()) {
					List<ClientHttpRequestInterceptor> interceptors = restTemplate
							.getInterceptors();
					// Avoid setting interceptor repeatedly.
					if (null != interceptors
							&& !interceptors.contains(metadataRestTemplateInterceptor)) {
						interceptors.add(metadataRestTemplateInterceptor);
						restTemplate.setInterceptors(interceptors);
					}
				}
			}
			return new MetadataRestTemplatePostProcessor(metadataRestTemplateInterceptor);
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext)
				throws BeansException {
			this.context = applicationContext;
		}

		public static class MetadataRestTemplatePostProcessor
				implements BeanPostProcessor {

			private MetadataRestTemplateInterceptor metadataRestTemplateInterceptor;

			MetadataRestTemplatePostProcessor(
					MetadataRestTemplateInterceptor metadataRestTemplateInterceptor) {
				this.metadataRestTemplateInterceptor = metadataRestTemplateInterceptor;
			}

			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return bean;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof RestTemplate) {
					RestTemplate restTemplate = (RestTemplate) bean;
					List<ClientHttpRequestInterceptor> interceptors = restTemplate
							.getInterceptors();
					// Avoid setting interceptor repeatedly.
					if (null != interceptors
							&& !interceptors.contains(metadataRestTemplateInterceptor)) {
						interceptors.add(this.metadataRestTemplateInterceptor);
						restTemplate.setInterceptors(interceptors);
					}
				}
				return bean;
			}

		}

	}

}
