/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 *  Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/BSD-3-Clause
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package com.tencent.cloud.polaris.config.spring.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ReflectionUtils;

/**
 *@author : wh
 *@date : 2022/6/28 09:18
 *@description:
 */
public abstract class AbstractPolarisProcessor implements BeanPostProcessor, PriorityOrdered {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		Class clazz = bean.getClass();
		for (Field field : findAllField(clazz)) {
			processField(bean, beanName, field);
		}
		for (Method method : findAllMethod(clazz)) {
			processMethod(bean, beanName, method);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * subclass should implement this method to process field
	 */
	protected abstract void processField(Object bean, String beanName, Field field);

	/**
	 * subclass should implement this method to process method
	 */
	protected abstract void processMethod(Object bean, String beanName, Method method);


	@Override
	public int getOrder() {
		//make it as late as possible
		return Ordered.LOWEST_PRECEDENCE;
	}

	private List<Field> findAllField(Class clazz) {
		final List<Field> res = new LinkedList<>();
		ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				res.add(field);
			}
		});
		return res;
	}

	private List<Method> findAllMethod(Class clazz) {
		final List<Method> res = new LinkedList<>();
		ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
			@Override
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				res.add(method);
			}
		});
		return res;
	}
}
