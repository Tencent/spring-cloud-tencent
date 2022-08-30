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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.properties.ConfigurationPropertiesBeans;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;

/**
 * Optimize {@link ConfigurationPropertiesRebinder}, only rebuild affected beans.
 * @author weihubeats 2022-7-10
 */
public class AffectedConfigurationPropertiesRebinder extends ConfigurationPropertiesRebinder {

	private ApplicationContext applicationContext;
	private Map<String, ConfigurationPropertiesBean> propertiesBeans = new HashMap<>();

	public AffectedConfigurationPropertiesRebinder(ConfigurationPropertiesBeans beans) {
		super(beans);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		super.setApplicationContext(applicationContext);

		this.applicationContext = applicationContext;

		propertiesBeans = ConfigurationPropertiesBean.getAll(applicationContext);
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
				}
			});
		});
	}
}
