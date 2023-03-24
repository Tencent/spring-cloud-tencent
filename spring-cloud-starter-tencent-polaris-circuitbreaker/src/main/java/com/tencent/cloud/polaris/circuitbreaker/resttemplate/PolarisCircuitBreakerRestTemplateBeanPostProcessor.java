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

package com.tencent.cloud.polaris.circuitbreaker.resttemplate;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * PolarisCircuitBreakerRestTemplateBeanPostProcessor.
 *
 * @author sean yu
 */
public class PolarisCircuitBreakerRestTemplateBeanPostProcessor implements MergedBeanDefinitionPostProcessor {

	private final ApplicationContext applicationContext;

	public PolarisCircuitBreakerRestTemplateBeanPostProcessor(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	private final ConcurrentHashMap<String, PolarisCircuitBreaker> cache = new ConcurrentHashMap<>();

	private void checkPolarisCircuitBreakerRestTemplate(PolarisCircuitBreaker polarisCircuitBreaker) {
		if (
				StringUtils.hasText(polarisCircuitBreaker.fallback()) &&
						!PolarisCircuitBreakerFallback.class.toGenericString().equals(polarisCircuitBreaker.fallbackClass().toGenericString())
		) {
			throw new IllegalArgumentException("PolarisCircuitBreaker's fallback and fallbackClass could not set at sametime !");
		}
	}

	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		if (checkAnnotated(beanDefinition, beanType, beanName)) {
			PolarisCircuitBreaker polarisCircuitBreaker;
			if (beanDefinition.getSource() instanceof StandardMethodMetadata) {
				polarisCircuitBreaker = ((StandardMethodMetadata) beanDefinition.getSource()).getIntrospectedMethod()
						.getAnnotation(PolarisCircuitBreaker.class);
			}
			else {
				polarisCircuitBreaker = beanDefinition.getResolvedFactoryMethod()
						.getAnnotation(PolarisCircuitBreaker.class);
			}
			checkPolarisCircuitBreakerRestTemplate(polarisCircuitBreaker);
			cache.put(beanName, polarisCircuitBreaker);
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (cache.containsKey(beanName)) {
			// add interceptor for each RestTemplate with @PolarisCircuitBreaker annotation
			StringBuilder interceptorBeanNamePrefix = new StringBuilder();
			PolarisCircuitBreaker polarisCircuitBreaker = cache.get(beanName);
			interceptorBeanNamePrefix
					.append(StringUtils.uncapitalize(
							PolarisCircuitBreaker.class.getSimpleName()))
					.append("_")
					.append(polarisCircuitBreaker.fallback())
					.append("_")
					.append(polarisCircuitBreaker.fallbackClass().getSimpleName());
			RestTemplate restTemplate = (RestTemplate) bean;
			String interceptorBeanName = interceptorBeanNamePrefix + "@" + bean;
			CircuitBreakerFactory circuitBreakerFactory = this.applicationContext.getBean(CircuitBreakerFactory.class);
			registerBean(interceptorBeanName, polarisCircuitBreaker, applicationContext, circuitBreakerFactory, restTemplate);
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
				.isAnnotated(PolarisCircuitBreaker.class.getName());
	}

	private void registerBean(String interceptorBeanName, PolarisCircuitBreaker polarisCircuitBreaker,
			ApplicationContext applicationContext, CircuitBreakerFactory circuitBreakerFactory, RestTemplate restTemplate) {
		// register PolarisCircuitBreakerRestTemplateInterceptor bean
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext
				.getAutowireCapableBeanFactory();
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(PolarisCircuitBreakerRestTemplateInterceptor.class);
		beanDefinitionBuilder.addConstructorArgValue(polarisCircuitBreaker);
		beanDefinitionBuilder.addConstructorArgValue(applicationContext);
		beanDefinitionBuilder.addConstructorArgValue(circuitBreakerFactory);
		beanDefinitionBuilder.addConstructorArgValue(restTemplate);
		BeanDefinition interceptorBeanDefinition = beanDefinitionBuilder
				.getRawBeanDefinition();
		beanFactory.registerBeanDefinition(interceptorBeanName,
				interceptorBeanDefinition);
	}

}
