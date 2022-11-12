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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.common.util.ReflectionUtils;
import com.tencent.polaris.api.utils.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.properties.ConfigurationPropertiesBeans;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Optimize {@link ConfigurationPropertiesRebinder}, only rebuild affected beans.
 *
 * @author weihubeats
 */
public class AffectedConfigurationPropertiesRebinder extends ConfigurationPropertiesRebinder {

	private static final Logger LOGGER = LoggerFactory.getLogger(AffectedConfigurationPropertiesRebinder.class);

	private ApplicationContext applicationContext;
	private Map<String, ConfigurationPropertiesBean> propertiesBeans = new HashMap<>();

	private final Map<String, Map<String, Object>> propertiesBeanDefaultValues = new ConcurrentHashMap<>();

	public AffectedConfigurationPropertiesRebinder(ConfigurationPropertiesBeans beans) {
		super(beans);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		super.setApplicationContext(applicationContext);

		this.applicationContext = applicationContext;

		propertiesBeans = ConfigurationPropertiesBean.getAll(applicationContext);
		initPropertiesBeanDefaultValues(propertiesBeans);
	}

	@Override
	public void onApplicationEvent(EnvironmentChangeEvent event) {
		if (this.applicationContext.equals(event.getSource())) {
			rebindAffectedBeans(event);
		}
	}

	private void rebindAffectedBeans(EnvironmentChangeEvent event) {
		Set<String> changedKeys = event.getKeys();

		if (CollectionUtils.isEmpty(changedKeys)) {
			return;
		}

		propertiesBeans.forEach((name, bean) -> {
			changedKeys.forEach(key -> {
				String propertiesPrefix = Objects.requireNonNull(AnnotationUtils.getValue(bean.getAnnotation()))
						.toString();
				if (key.startsWith(propertiesPrefix)) {
					rebind(name);
					rebindDefaultValue(name, key);
				}
			});
		});
	}

	private void rebindDefaultValue(String beanName, String key) {
		String changeValue = applicationContext.getEnvironment().getProperty(key);
		if (StringUtils.hasLength(changeValue)) {
			return;
		}

		Map<String, Object> defaultValues = propertiesBeanDefaultValues.get(beanName);
		if (MapUtils.isEmpty(defaultValues)) {
			return;
		}
		try {
			String fieldName = key.substring(key.lastIndexOf(".") + 1);

			Object bean = applicationContext.getBean(beanName);
			Field field = ReflectionUtils.findField(bean.getClass(), fieldName);
			if (field != null) {
				field.setAccessible(true);
				field.set(bean, defaultValues.get(fieldName));
			}
		}
		catch (Exception e) {
			LOGGER.error("[SCT Config] rebind default value error, bean = {}, key = {}", beanName, key);
		}
	}

	private void initPropertiesBeanDefaultValues(Map<String, ConfigurationPropertiesBean> propertiesBeans) {
		if (MapUtils.isEmpty(propertiesBeans)) {
			return;
		}

		for (ConfigurationPropertiesBean propertiesBean : propertiesBeans.values()) {
			Map<String, Object> defaultValues = new HashMap<>();
			try {
				Object instance = propertiesBean.getInstance().getClass().getDeclaredConstructor((Class<?>[]) null).newInstance();
				ReflectionUtils.doWithFields(instance.getClass(), field -> {
					try {
						field.setAccessible(true);
						defaultValues.put(field.getName(), field.get(instance));
					}
					catch (Exception ignored) {
					}
				}, field -> {
					int modifiers = field.getModifiers();
					return !Modifier.isFinal(modifiers) && !Modifier.isStatic(modifiers) && ReflectionUtils.writableBeanField(field);
				});
			}
			catch (Exception ignored) {
			}

			propertiesBeanDefaultValues.put(propertiesBean.getName(), defaultValues);
		}
	}
}
