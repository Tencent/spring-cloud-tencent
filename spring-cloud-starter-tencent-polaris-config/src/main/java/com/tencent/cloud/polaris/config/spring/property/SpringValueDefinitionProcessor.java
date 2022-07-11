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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 *@author : wh
 *@date : 2022/7/10 14:25
 *@description:
 */
public class SpringValueDefinitionProcessor implements BeanDefinitionRegistryPostProcessor {
	private static final Map<BeanDefinitionRegistry, Multimap<String, SpringValueDefinition>> beanName2SpringValueDefinitions =
			Maps.newConcurrentMap();
	private static final Set<BeanDefinitionRegistry> PROPERTY_VALUES_PROCESSED_BEAN_FACTORIES = Sets.newConcurrentHashSet();

	private final PlaceholderHelper placeholderHelper;

	private PolarisConfigProperties polarisConfigProperties;

	public SpringValueDefinitionProcessor(PlaceholderHelper placeholderHelper, PolarisConfigProperties polarisConfigProperties) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.placeholderHelper = placeholderHelper;
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		if (polarisConfigProperties.isAutoRefresh()) {
			processPropertyValues(registry);
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}

	public static Multimap<String, SpringValueDefinition> getBeanName2SpringValueDefinitions(BeanDefinitionRegistry registry) {
		Multimap<String, SpringValueDefinition> springValueDefinitions = beanName2SpringValueDefinitions.get(registry);
		if (springValueDefinitions == null) {
			springValueDefinitions = LinkedListMultimap.create();
		}

		return springValueDefinitions;
	}

	private void processPropertyValues(BeanDefinitionRegistry beanRegistry) {
		if (!PROPERTY_VALUES_PROCESSED_BEAN_FACTORIES.add(beanRegistry)) {
			// already initialized
			return;
		}

		if (!beanName2SpringValueDefinitions.containsKey(beanRegistry)) {
			beanName2SpringValueDefinitions.put(beanRegistry, LinkedListMultimap.create());
		}

		Multimap<String, SpringValueDefinition> springValueDefinitions = beanName2SpringValueDefinitions.get(beanRegistry);

		String[] beanNames = beanRegistry.getBeanDefinitionNames();
		for (String beanName : beanNames) {
			BeanDefinition beanDefinition = beanRegistry.getBeanDefinition(beanName);
			MutablePropertyValues mutablePropertyValues = beanDefinition.getPropertyValues();
			List<PropertyValue> propertyValues = mutablePropertyValues.getPropertyValueList();
			for (PropertyValue propertyValue : propertyValues) {
				Object value = propertyValue.getValue();
				if (!(value instanceof TypedStringValue)) {
					continue;
				}
				String placeholder = ((TypedStringValue) value).getValue();
				Set<String> keys = placeholderHelper.extractPlaceholderKeys(placeholder);

				if (keys.isEmpty()) {
					continue;
				}

				for (String key : keys) {
					springValueDefinitions.put(beanName, new SpringValueDefinition(key, placeholder, propertyValue.getName()));
				}
			}
		}
	}
}