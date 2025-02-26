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

package com.tencent.cloud.polaris.config.spring.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

/**
 * Get spring bean properties and methods.
 *
 * @author weihubeats 2022-7-10
 */
public abstract class PolarisProcessor implements BeanPostProcessor, PriorityOrdered, ApplicationContextAware {

	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public Object postProcessBeforeInitialization(Object bean, @NonNull String beanName)
			throws BeansException {
		Class<?> clazz = bean.getClass();

		boolean isRefreshScope = false;

		try {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			if ("refresh".equals(beanDefinition.getScope())) {
				isRefreshScope = true;
			}
		}
		catch (Exception ignored) {
			// ignore
		}

		for (Field field : findAllField(clazz)) {
			processField(bean, beanName, field, isRefreshScope);
		}
		for (Method method : findAllMethod(clazz)) {
			processMethod(bean, beanName, method, isRefreshScope);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		return bean;
	}

	/**
	 * subclass should implement this method to process field.
	 * @param bean bean
	 * @param beanName beanName
	 * @param field field
	 */
	protected abstract void processField(Object bean, String beanName, Field field, boolean isRefreshScope);

	/**
	 * subclass should implement this method to process method.
	 * @param bean bean
	 * @param beanName beanName
	 * @param method method
	 */
	protected abstract void processMethod(Object bean, String beanName, Method method, boolean isRefreshScope);


	@Override
	public int getOrder() {
		//make it as late as possible
		return Ordered.LOWEST_PRECEDENCE;
	}

	protected List<Field> findAllField(Class<?> clazz) {
		final List<Field> res = new LinkedList<>();
		ReflectionUtils.doWithFields(clazz, res::add);
		return res;
	}

	protected List<Method> findAllMethod(Class<?> clazz) {
		final List<Method> res = new LinkedList<>();
		ReflectionUtils.doWithMethods(clazz, res::add);
		return res;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
	}
}
