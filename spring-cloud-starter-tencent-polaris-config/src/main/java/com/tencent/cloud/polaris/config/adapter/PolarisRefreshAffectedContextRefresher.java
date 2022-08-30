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

package com.tencent.cloud.polaris.config.adapter;

import java.util.Collection;
import java.util.Set;

import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.spring.property.PlaceholderHelper;
import com.tencent.cloud.polaris.config.spring.property.SpringValue;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;

/**
 * 1. The refresh of @Value properties is implemented through reflection.
 * 2. Implement @ConfigurationProperties bean property refresh via EnvironmentChangeEvent,
 * while rebuilding only beans with property changes.
 *
 * @author lingxiao.wlx
 */
public class PolarisRefreshAffectedContextRefresher extends PolarisConfigPropertyAutoRefresher implements ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisRefreshAffectedContextRefresher.class);

	private final SpringValueRegistry springValueRegistry;

	private final PlaceholderHelper placeholderHelper;

	private ConfigurableApplicationContext context;

	private ConfigurableBeanFactory beanFactory;

	private TypeConverter typeConverter;

	public PolarisRefreshAffectedContextRefresher(PolarisConfigProperties polarisConfigProperties,
			PolarisPropertySourceManager polarisPropertySourceManager, SpringValueRegistry springValueRegistry,
			PlaceholderHelper placeholderHelper) {
		super(polarisConfigProperties, polarisPropertySourceManager);
		this.springValueRegistry = springValueRegistry;
		this.placeholderHelper = placeholderHelper;
	}

	@Override
	public void refreshSpringValue(String changedKey) {
		Collection<SpringValue> targetValues = springValueRegistry.get(beanFactory, changedKey);
		if (targetValues == null || targetValues.isEmpty()) {
			return;
		}
		// update the attribute with @Value annotation
		for (SpringValue val : targetValues) {
			updateSpringValue(val);
		}
	}

	@Override
	public void refreshConfigurationProperties(Set<String> changeKeys) {
		context.publishEvent(new EnvironmentChangeEvent(context, changeKeys));
	}

	private void updateSpringValue(SpringValue springValue) {
		try {
			Object value = resolvePropertyValue(springValue);
			springValue.update(value);

			LOGGER.info("[SCT Config] Auto update polaris changed value successfully, new value: {}, {}", value,
					springValue);
		}
		catch (Throwable ex) {
			LOGGER.error("[SCT Config] Auto update polaris changed value failed, {}", springValue.toString(), ex);
		}
	}

	/**
	 * Logic transplanted from DefaultListableBeanFactory.
	 *
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#doResolveDependency(org.springframework.beans.factory.config.DependencyDescriptor,
	 * java.lang.String, java.util.Set, org.springframework.beans.TypeConverter)
	 */
	private Object resolvePropertyValue(SpringValue springValue) {
		// value will never be null
		Object value = placeholderHelper
				.resolvePropertyValue(beanFactory, springValue.getBeanName(), springValue.getPlaceholder());

		value = springValue.isField() ? this.typeConverter.convertIfNecessary(value, springValue.getTargetType(), springValue.getField()) :
				this.typeConverter.convertIfNecessary(value, springValue.getTargetType(),
						springValue.getMethodParameter());

		return value;
	}

	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		this.context = (ConfigurableApplicationContext) applicationContext;
		this.beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
		this.typeConverter = this.beanFactory.getTypeConverter();
	}
}
