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

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

public class ContextGatewayFilterFactory extends AbstractGatewayFilterFactory<ContextGatewayFilterFactory.Config> {

	private ContextGatewayPropertiesManager manager;

	public ContextGatewayFilterFactory(ContextGatewayPropertiesManager manager)  {
		super(Config.class);
		this.manager = manager;
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList("group");
	}

	@Override
	public GatewayFilter apply(Config config) {
		return new ContextGatewayFilter(manager, config);
	}

	public static class Config {
		private String group;

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
		}
	}


}
