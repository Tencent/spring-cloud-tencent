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
 *
 */

package com.tencent.cloud.polaris.config.adapter;

import java.util.Set;

import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import com.tencent.polaris.configuration.api.core.ConfigFileService;

import org.springframework.beans.BeansException;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The default implement of Spring Cloud refreshes the entire Spring Context.
 * The disadvantage is that the entire context is rebuilt, which has a large impact and low performance.
 *
 * @author lingxiao.wlx
 */
public class PolarisRefreshEntireContextRefresher extends PolarisConfigPropertyAutoRefresher implements ApplicationContextAware {

	private final ContextRefresher contextRefresher;

	private final SpringValueRegistry springValueRegistry;

	private ConfigurableApplicationContext context;

	public PolarisRefreshEntireContextRefresher(PolarisConfigProperties polarisConfigProperties,
			SpringValueRegistry springValueRegistry, ConfigFileService configFileService, ContextRefresher contextRefresher) {

		super(polarisConfigProperties, configFileService);
		this.springValueRegistry = springValueRegistry;
		this.contextRefresher = contextRefresher;
	}

	@Override
	public void refreshSpringValue(String changedKey) {
		// do nothing,all config will be refreshed by contextRefresher.refresh
	}

	@Override
	public void refreshConfigurationProperties(Set<String> changeKeys) {
		boolean needRefreshContext = false;
		for (String changedKey : changeKeys) {
			boolean inRefreshScope = springValueRegistry.isRefreshScopeKey(changedKey);
			if (inRefreshScope) {
				needRefreshContext = true;
				break;
			}
		}
		if (needRefreshContext) {
			contextRefresher.refresh();
		}
		else {
			context.publishEvent(new EnvironmentChangeEvent(context, changeKeys));
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = (ConfigurableApplicationContext) applicationContext;
	}
}
