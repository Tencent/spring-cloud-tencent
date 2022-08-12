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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.tencent.cloud.polaris.config.enums.RefreshBehavior;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.properties.ConfigurationPropertiesBeans;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import static com.tencent.cloud.polaris.config.condition.NonDefaultBehaviorCondition.POLARIS_CONFIG_REFRESH_BEHAVIOR;
import static com.tencent.cloud.polaris.config.enums.RefreshBehavior.ALL_BEANS;

/**
 * Extend {@link ConfigurationPropertiesRebinder}.
 * <p>
 * Spring team doesn't seem to support single {@link ConfigurationPropertiesBean} refresh.
 * <p>
 * SmartConfigurationPropertiesRebinder can refresh specific
 * {@link ConfigurationPropertiesBean} base on the change keys.
 * <p>
 * <strong> NOTE: We still use Spring's default behavior (full refresh) as default
 * behavior, This feature can be considered an advanced feature, it may not be as stable
 * as the default behavior. </strong>
 * <code><a href=https://github.com/alibaba/spring-cloud-alibaba/blob/2.2.x/spring-cloud-alibaba-starters/spring-cloud-starter-alibaba-nacos-config/src/main/java/com/alibaba/cloud/nacos/refresh/SmartConfigurationPropertiesRebinder.java>
 * SmartConfigurationPropertiesRebinder</a></code>
 *
 * @author weihubeats 2022-7-10
 */
public class SmartConfigurationPropertiesRebinder extends ConfigurationPropertiesRebinder {

	private static final String BEANS = "beans";

	private Map<String, ConfigurationPropertiesBean> beanMap;

	private ApplicationContext applicationContext;

	private RefreshBehavior refreshBehavior;

	public SmartConfigurationPropertiesRebinder(ConfigurationPropertiesBeans beans) {
		super(beans);
		fillBeanMap(beans);
	}

	@SuppressWarnings("unchecked")
	private void fillBeanMap(ConfigurationPropertiesBeans beans) {
		this.beanMap = new HashMap<>();
		Field field = ReflectionUtils.findField(beans.getClass(), BEANS);
		if (field != null) {
			field.setAccessible(true);
			this.beanMap.putAll((Map<String, ConfigurationPropertiesBean>) Optional
					.ofNullable(ReflectionUtils.getField(field, beans))
					.orElse(Collections.emptyMap()));
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		super.setApplicationContext(applicationContext);
		this.applicationContext = applicationContext;
		this.refreshBehavior = this.applicationContext.getEnvironment().getProperty(
				POLARIS_CONFIG_REFRESH_BEHAVIOR, RefreshBehavior.class,
				ALL_BEANS);
	}

	@Override
	public void onApplicationEvent(EnvironmentChangeEvent event) {
		if (this.applicationContext.equals(event.getSource())
				// Backwards compatible
				|| event.getKeys().equals(event.getSource())) {
			switch (refreshBehavior) {
			case SPECIFIC_BEAN:
				rebindSpecificBean(event);
				break;
			case ALL_BEANS:
			default:
				rebind();
				break;
			}
		}
	}

	private void rebindSpecificBean(EnvironmentChangeEvent event) {
		Set<String> refreshedSet = new HashSet<>();
		beanMap.forEach((name, bean) -> event.getKeys().forEach(changeKey -> {
			String prefix = Objects.requireNonNull(AnnotationUtils.getValue(bean.getAnnotation())).toString();
			// prevent multiple refresh one ConfigurationPropertiesBean.
			if (changeKey.startsWith(prefix) && refreshedSet.add(name)) {
				rebind(name);
			}
		}));
	}

}
