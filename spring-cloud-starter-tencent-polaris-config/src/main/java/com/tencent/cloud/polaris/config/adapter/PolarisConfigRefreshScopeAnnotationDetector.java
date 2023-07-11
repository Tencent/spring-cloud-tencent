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

package com.tencent.cloud.polaris.config.adapter;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.NonNull;

/**
 * Mainly used to detect whether the annotation class {@link org.springframework.cloud.context.config.annotation.RefreshScope}
 * exists, and whether the user has configured beans using this annotation in their business system.
 * If the annotation {@code @RefreshScope} exists and is used, the auto-optimization will be triggered
 * in listener {@link com.tencent.cloud.polaris.config.listener.PolarisConfigRefreshOptimizationListener}.
 *
 * <p>This bean will only be created and initialized when the config refresh type is {@code RefreshType.REFLECT}.
 *
 * @author jarvisxiong
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PolarisConfigRefreshScopeAnnotationDetector implements BeanPostProcessor, InitializingBean, PriorityOrdered {

	private final AtomicBoolean isRefreshScopeAnnotationUsed = new AtomicBoolean(false);

	private Class refreshScopeAnnotationClass;

	private String annotatedRefreshScopeBeanName;

	@Override
	public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName)
			throws BeansException {
		if (isRefreshScopeAnnotationUsed() || refreshScopeAnnotationClass == null) {
			return bean;
		}
		Annotation[] refreshScopeAnnotations = bean.getClass().getAnnotationsByType(refreshScopeAnnotationClass);
		if (refreshScopeAnnotations.length > 0) {
			if (isRefreshScopeAnnotationUsed.compareAndSet(false, true)) {
				annotatedRefreshScopeBeanName = beanName;
			}
		}
		return bean;
	}

	@Override
	public void afterPropertiesSet() {
		try {
			refreshScopeAnnotationClass = Class.forName(
					"org.springframework.cloud.context.config.annotation.RefreshScope",
					false,
					getClass().getClassLoader());
		}
		catch (ClassNotFoundException ignored) {
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	public boolean isRefreshScopeAnnotationUsed() {
		return isRefreshScopeAnnotationUsed.get();
	}

	public String getAnnotatedRefreshScopeBeanName() {
		return annotatedRefreshScopeBeanName;
	}
}
