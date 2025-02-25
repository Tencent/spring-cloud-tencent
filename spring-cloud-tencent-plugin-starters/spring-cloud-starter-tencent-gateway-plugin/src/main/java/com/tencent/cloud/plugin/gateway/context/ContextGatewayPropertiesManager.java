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
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClient;
import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.AntPathMatcher;

public class ContextGatewayPropertiesManager {

	private static final Logger logger = LoggerFactory.getLogger(ContextGatewayPropertiesManager.class);
	/**
	 * context -> {path key -> route}.
	 */
	private volatile ConcurrentHashMap<String, Map<String, GroupContext.ContextRoute>> groupPathRouteMap = new ConcurrentHashMap<>();
	/**
	 * context -> {wildcard path key -> route}.
	 */
	private volatile ConcurrentHashMap<String, Map<String, GroupContext.ContextRoute>> groupWildcardPathRouteMap = new ConcurrentHashMap<>();

	private Map<String, GroupContext> groups = new HashMap<>();

	private AntPathMatcher antPathMatcher = new AntPathMatcher();

	public Map<String, Map<String, GroupContext.ContextRoute>> getGroupPathRouteMap() {
		return groupPathRouteMap;
	}

	public void setGroupRouteMap(Map<String, GroupContext> groups) {

		ConcurrentHashMap<String, Map<String, GroupContext.ContextRoute>> newGroupPathRouteMap = new ConcurrentHashMap<>();
		ConcurrentHashMap<String, Map<String, GroupContext.ContextRoute>> newGroupWildcardPathRouteMap = new ConcurrentHashMap<>();
		if (groups != null) {
			for (Map.Entry<String, GroupContext> entry : groups.entrySet()) {
				Map<String, GroupContext.ContextRoute> newGroupPathRoute = new HashMap<>();
				Map<String, GroupContext.ContextRoute> newGroupWildcardPathRoute = new HashMap<>();
				for (GroupContext.ContextRoute route : entry.getValue().getRoutes()) {
					String path = route.getPath();
					// convert path parameter to group wildcard path
					if (path.contains("{") && path.contains("}") || path.contains("*")) {
						newGroupWildcardPathRoute.put(buildPathKey(entry.getValue(), route), route);
					}
					else {
						newGroupPathRoute.put(buildPathKey(entry.getValue(), route), route);
					}
				}
				newGroupWildcardPathRouteMap.put(entry.getKey(), newGroupWildcardPathRoute);
				newGroupPathRouteMap.put(entry.getKey(), newGroupPathRoute);
			}
		}
		this.groupPathRouteMap = newGroupPathRouteMap;
		this.groupWildcardPathRouteMap = newGroupWildcardPathRouteMap;
		this.groups = groups;
	}

	public Map<String, GroupContext> getGroups() {
		return groups;
	}

	public GroupContext.ContextRoute getGroupPathRoute(String group, String path) {
		Map<String, GroupContext.ContextRoute> groupPathRouteMap = this.groupPathRouteMap.get(group);
		if (groupPathRouteMap != null && groupPathRouteMap.containsKey(path)) {
			return groupPathRouteMap.get(path);
		}

		Map<String, GroupContext.ContextRoute> groupWildcardPathRouteMap = this.groupWildcardPathRouteMap.get(group);
		if (groupWildcardPathRouteMap != null) {
			for (Map.Entry<String, GroupContext.ContextRoute> entry : groupWildcardPathRouteMap.entrySet()) {
				boolean matched = antPathMatcher.match(entry.getKey(), path);
				if (matched) {
					return entry.getValue();
				}
			}
		}
		return null;
	}

	public void eagerLoad(PolarisDiscoveryClient polarisDiscoveryClient,
			PolarisReactiveDiscoveryClient polarisReactiveDiscoveryClient) {
		for (Map<String, GroupContext.ContextRoute> contextRouteMap : groupPathRouteMap.values()) {
			for (GroupContext.ContextRoute contextRoute : contextRouteMap.values()) {
				eagerLoadFromRoute(contextRoute, polarisDiscoveryClient, polarisReactiveDiscoveryClient);
			}
		}
		for (Map<String, GroupContext.ContextRoute> contextRouteMap : groupWildcardPathRouteMap.values()) {
			for (GroupContext.ContextRoute contextRoute : contextRouteMap.values()) {
				eagerLoadFromRoute(contextRoute, polarisDiscoveryClient, polarisReactiveDiscoveryClient);
			}
		}
	}

	private void eagerLoadFromRoute(GroupContext.ContextRoute contextRoute, PolarisDiscoveryClient polarisDiscoveryClient,
			PolarisReactiveDiscoveryClient polarisReactiveDiscoveryClient) {
		String namespace = contextRoute.getNamespace();
		String service = contextRoute.getService();
		if (StringUtils.isNotEmpty(namespace) && StringUtils.isNotEmpty(service)) {
			logger.info("[{},{}] eager-load start", namespace, service);
			MetadataContextHolder.get().putFragmentContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
					MetadataConstant.POLARIS_TARGET_NAMESPACE, namespace);

			if (polarisDiscoveryClient != null) {
				polarisDiscoveryClient.getInstances(service);
			}
			else if (polarisReactiveDiscoveryClient != null) {
				polarisReactiveDiscoveryClient.getInstances(service).subscribe();
			}
			else {
				logger.warn("[{}] no discovery client found.", service);
			}
			logger.info("[{},{}] eager-load end", namespace, service);
		}
	}

	private String buildPathKey(GroupContext groupContext, GroupContext.ContextRoute route)  {
		switch (groupContext.getPredicate().getApiType()) {
			case MS:
				return String.format("%s|/%s/%s%s", route.getMethod(), route.getNamespace(), route.getService(), route.getPath());
			case EXTERNAL:
			default:
				return String.format("%s|%s", route.getMethod(), route.getPath());
		}
	}

}
