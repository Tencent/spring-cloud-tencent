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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.core.style.ToStringCreator;

@ConfigurationProperties(ContextGatewayProperties.PREFIX)
public class ContextGatewayProperties {

	/**
	 * Properties prefix.
	 */
	public static final String PREFIX = "spring.cloud.tencent.gateway";

	private final Log logger = LogFactory.getLog(getClass());

	private Map<String, RouteDefinition> routes = new HashMap<>();

	private Map<String, GroupContext> groups = new HashMap<>();

	public Map<String, RouteDefinition> getRoutes() {
		return routes;
	}

	public void setRoutes(Map<String, RouteDefinition> routes) {
		this.routes = routes;
		if (routes != null && routes.size() > 0 && logger.isDebugEnabled()) {
			logger.debug("Routes supplied from Gateway Properties: " + routes);
		}
	}

	public Map<String, GroupContext> getGroups() {
		return groups;
	}

	public void setGroups(Map<String, GroupContext> groups) {
		this.groups = groups;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("routes", routes)
				.append("groups", groups).toString();

	}

}
