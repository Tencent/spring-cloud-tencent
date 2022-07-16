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

package com.tencent.cloud.polaris.config.annotation;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.tencent.cloud.polaris.config.listener.ConfigChangeEvent;
import com.tencent.cloud.polaris.config.listener.ConfigChangeListener;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import static com.tencent.cloud.polaris.config.listener.PolarisConfigListenerContext.addChangeListener;

/**
 * {@link PolarisConfigAnnotationProcessor} implementation for spring .
 * <p>This source file was reference from：
 * <code><a href=https://github.com/apolloconfig/apollo/blob/master/apollo-client/src/main/java/com/ctrip/framework/apollo/spring/annotation/ApolloAnnotationProcessor.java>
 *     ApolloAnnotationProcessor</a></code>
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-06-07
 */
public class PolarisConfigAnnotationProcessor implements BeanPostProcessor, PriorityOrdered {

	@Override
	public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName)
			throws BeansException {
		Class<?> clazz = bean.getClass();
		for (Method method : findAllMethod(clazz)) {
			this.processPolarisConfigChangeListener(bean, method);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		return bean;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	private List<Method> findAllMethod(Class<?> clazz) {
		final List<Method> res = new LinkedList<>();
		ReflectionUtils.doWithMethods(clazz, res::add);
		return res;
	}

	private void processPolarisConfigChangeListener(final Object bean, final Method method) {
		PolarisConfigKVFileChangeListener annotation = AnnotationUtils
				.findAnnotation(method, PolarisConfigKVFileChangeListener.class);
		if (annotation == null) {
			return;
		}
		Class<?>[] parameterTypes = method.getParameterTypes();
		Preconditions.checkArgument(parameterTypes.length == 1,
				"Invalid number of parameters: %s for method: %s, should be 1", parameterTypes.length,
				method);
		Preconditions.checkArgument(ConfigChangeEvent.class.isAssignableFrom(parameterTypes[0]),
				"Invalid parameter type: %s for method: %s, should be ConfigChangeEvent", parameterTypes[0],
				method);

		ReflectionUtils.makeAccessible(method);
		String[] annotatedInterestedKeys = annotation.interestedKeys();
		String[] annotatedInterestedKeyPrefixes = annotation.interestedKeyPrefixes();

		ConfigChangeListener configChangeListener = changeEvent -> ReflectionUtils.invokeMethod(method, bean, changeEvent);

		Set<String> interestedKeys =
				annotatedInterestedKeys.length > 0 ? Sets.newHashSet(annotatedInterestedKeys) : null;
		Set<String> interestedKeyPrefixes =
				annotatedInterestedKeyPrefixes.length > 0 ? Sets.newHashSet(annotatedInterestedKeyPrefixes) : null;

		addChangeListener(configChangeListener, interestedKeys, interestedKeyPrefixes);
	}
}
