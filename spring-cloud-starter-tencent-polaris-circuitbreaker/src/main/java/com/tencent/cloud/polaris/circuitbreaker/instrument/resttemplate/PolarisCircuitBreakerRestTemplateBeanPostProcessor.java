/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.polaris.circuitbreaker.instrument.resttemplate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ApplicationContext;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * PolarisCircuitBreakerRestTemplateBeanPostProcessor.
 *
 * @author sean yu
 */
public class PolarisCircuitBreakerRestTemplateBeanPostProcessor implements MergedBeanDefinitionPostProcessor {

	private final ApplicationContext applicationContext;
	private final Set<String> cache = Collections.synchronizedSet(new HashSet<>());

	public PolarisCircuitBreakerRestTemplateBeanPostProcessor(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		if (checkAnnotated(beanDefinition, beanType, beanName)) {
			cache.add(beanName);
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (cache.contains(beanName)) {
			String interceptorBeanNamePrefix = StringUtils.uncapitalize("PolarisCircuitBreaker");
			RestTemplate restTemplate = (RestTemplate) bean;
			String interceptorBeanName = interceptorBeanNamePrefix + "@" + bean;
			CircuitBreakerFactory circuitBreakerFactory = this.applicationContext.getBean(CircuitBreakerFactory.class);
			registerBean(interceptorBeanName, applicationContext, circuitBreakerFactory, restTemplate);
			PolarisCircuitBreakerRestTemplateInterceptor polarisCircuitBreakerRestTemplateInterceptor = applicationContext
					.getBean(interceptorBeanName, PolarisCircuitBreakerRestTemplateInterceptor.class);
			restTemplate.getInterceptors().add(0, polarisCircuitBreakerRestTemplateInterceptor);
		}
		return bean;
	}

	private boolean checkAnnotated(RootBeanDefinition beanDefinition,
			Class<?> beanType, String beanName) {
		return beanName != null && beanType == RestTemplate.class
				&& beanDefinition.getSource() instanceof MethodMetadata
				&& ((MethodMetadata) beanDefinition.getSource())
				.isAnnotated(LoadBalanced.class.getName());
	}

	private void registerBean(String interceptorBeanName, ApplicationContext applicationContext,
			CircuitBreakerFactory circuitBreakerFactory, RestTemplate restTemplate) {
		// register PolarisCircuitBreakerRestTemplateInterceptor bean
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext
				.getAutowireCapableBeanFactory();
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(PolarisCircuitBreakerRestTemplateInterceptor.class);
		beanDefinitionBuilder.addConstructorArgValue(circuitBreakerFactory);
		beanDefinitionBuilder.addConstructorArgValue(restTemplate);
		BeanDefinition interceptorBeanDefinition = beanDefinitionBuilder
				.getRawBeanDefinition();
		beanFactory.registerBeanDefinition(interceptorBeanName,
				interceptorBeanDefinition);
	}

}
