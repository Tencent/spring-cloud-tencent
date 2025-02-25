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
 */

package com.tencent.cloud.plugin.gateway.context;

import com.tencent.cloud.polaris.config.annotation.PolarisConfigKVFileChangeListener;
import com.tencent.cloud.polaris.config.listener.ConfigChangeEvent;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;

public class GatewayConfigChangeListener {

	private ApplicationEventPublisher publisher;

	private ContextGatewayPropertiesManager manager;

	private Environment environment;

	public GatewayConfigChangeListener(ContextGatewayPropertiesManager manager,
			ApplicationEventPublisher publisher, Environment environment)  {
		this.manager = manager;
		this.publisher = publisher;
		this.environment = environment;
	}

	@PolarisConfigKVFileChangeListener(interestedKeyPrefixes = ContextGatewayProperties.PREFIX)
	public void onChangeTencentGatewayProperties(ConfigChangeEvent event) {
		Binder binder = Binder.get(environment);
		BindResult<ContextGatewayProperties> result = binder.bind(ContextGatewayProperties.PREFIX, ContextGatewayProperties.class);
		manager.setGroupRouteMap(result.get().getGroups());
		this.publisher.publishEvent(new RefreshRoutesEvent(event));
	}

	@PolarisConfigKVFileChangeListener(interestedKeyPrefixes = GatewayProperties.PREFIX)
	public void onChangeGatewayConfigChangeListener(ConfigChangeEvent event) {
		this.publisher.publishEvent(new RefreshRoutesEvent(event));
	}
}
