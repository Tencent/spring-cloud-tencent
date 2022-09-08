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

package com.tencent.cloud.polaris.config.spring.property;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;

/**
 * Spring @Value method info.
 * <p>
 * This source file was originally from:
 * <code><a href=https://github.com/apolloconfig/apollo/blob/master/apollo-client/src/main/java/com/ctrip/framework/apollo/spring/property/SpringValue.java>
 *     SpringValue</a></code>
 *
 * @author weihubeats 2022-7-10
 */
public class SpringValue {

	private final WeakReference<Object> beanRef;
	private final String beanName;
	private final String key;
	private final String placeholder;
	private final Class<?> targetType;
	private MethodParameter methodParameter;
	private Field field;

	public SpringValue(String key, String placeholder, Object bean, String beanName, Field field) {
		this.beanRef = new WeakReference<>(bean);
		this.beanName = beanName;
		this.field = field;
		this.key = key;
		this.placeholder = placeholder;
		this.targetType = field.getType();
	}

	public SpringValue(String key, String placeholder, Object bean, String beanName, Method method) {
		this.beanRef = new WeakReference<>(bean);
		this.beanName = beanName;
		this.methodParameter = new MethodParameter(method, 0);
		this.key = key;
		this.placeholder = placeholder;
		Class<?>[] paramTps = method.getParameterTypes();
		this.targetType = paramTps[0];
	}

	public void update(Object newVal) throws IllegalAccessException, InvocationTargetException {
		if (isField()) {
			injectField(newVal);
		}
		else {
			injectMethod(newVal);
		}
	}

	private void injectField(Object newVal) throws IllegalAccessException {
		Object bean = beanRef.get();
		if (bean == null) {
			return;
		}
		boolean accessible = field.isAccessible();
		field.setAccessible(true);
		field.set(bean, newVal);
		field.setAccessible(accessible);
	}

	private void injectMethod(Object newVal)
			throws InvocationTargetException, IllegalAccessException {
		Object bean = beanRef.get();
		if (bean == null || methodParameter.getMethod() == null) {
			return;
		}
		methodParameter.getMethod().invoke(bean, newVal);
	}

	public String getBeanName() {
		return beanName;
	}

	public Class<?> getTargetType() {
		return targetType;
	}

	public String getPlaceholder() {
		return this.placeholder;
	}

	public MethodParameter getMethodParameter() {
		return methodParameter;
	}

	public boolean isField() {
		return this.field != null;
	}

	public Field getField() {
		return field;
	}

	boolean isTargetBeanValid() {
		return beanRef.get() != null;
	}

	@Override
	public String toString() {
		Object bean = beanRef.get();
		if (bean == null) {
			return "";
		}
		if (isField()) {
			return String
					.format("key: %s, beanName: %s, field: %s.%s", key, beanName, bean.getClass()
							.getName(), field.getName());
		}
		if (null != methodParameter.getMethod()) {
			return String.format("key: %s, beanName: %s, method: %s.%s", key, beanName, bean.getClass().getName(),
					methodParameter.getMethod().getName());
		}
		else {
			return String.format("key: %s, beanName: %s", key, beanName);
		}
	}
}
