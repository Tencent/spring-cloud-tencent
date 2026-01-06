/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClient;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.gateway.core.constant.PluginScopeType;
import com.tencent.tsf.gateway.core.constant.PluginType;
import com.tencent.tsf.gateway.core.exception.TsfGatewayError;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import com.tencent.tsf.gateway.core.model.PluginArgInfo;
import com.tencent.tsf.gateway.core.model.PluginDetail;
import com.tencent.tsf.gateway.core.model.PluginInfo;
import com.tencent.tsf.gateway.core.model.PluginInstanceInfo;
import com.tencent.tsf.gateway.core.util.PluginUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shade.polaris.org.apache.commons.beanutils.BeanUtils;

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
	/**
	 * context -> {unit path key -> route}.
	 */
	private volatile ConcurrentHashMap<String, Map<String, GroupContext.ContextRoute>> groupUnitPathRouteMap = new ConcurrentHashMap<>();
	/**
	 * context -> {unit wildcard path key -> route}.
	 */
	private volatile ConcurrentHashMap<String, Map<String, GroupContext.ContextRoute>> groupUnitWildcardPathRouteMap = new ConcurrentHashMap<>();
	/**
	 * group -> plugin info.
	 */
	private volatile ConcurrentHashMap<String, PluginInstanceInfo> groupPluginInfoMap = new ConcurrentHashMap<>();
	/**
	 * api id -> plugin info.
	 */
	private volatile ConcurrentHashMap<String, PluginInstanceInfo> apiPluginInfoMap = new ConcurrentHashMap<>();
	/**
	 * plugin id -> plugin info.
	 */
	private volatile ConcurrentHashMap<String, PluginInfo> gatewayPluginInfoMap = new ConcurrentHashMap<>();

	private Map<String, GroupContext> groups = new HashMap<>();

	private List<PathRewrite> pathRewrites = new ArrayList<>();

	private List<PluginInstanceInfo> plugins = new ArrayList<>();

	private final AntPathMatcher antPathMatcher = new AntPathMatcher();

	public Map<String, Map<String, GroupContext.ContextRoute>> getGroupPathRouteMap() {
		return groupPathRouteMap;
	}

	public Map<String, Map<String, GroupContext.ContextRoute>> getGroupWildcardPathRouteMap() {
		return groupWildcardPathRouteMap;
	}

	public List<PathRewrite> getPathRewrites() {
		return pathRewrites;
	}

	public void setPathRewrites(List<PathRewrite> pathRewrites) {
		this.pathRewrites = pathRewrites;
	}

	public List<PluginInstanceInfo> getPlugins() {
		return plugins;
	}

	public void setPlugins(List<PluginInstanceInfo> plugins) {
		this.plugins = plugins;
	}

	public void refreshPlugins(List<PluginInstanceInfo> plugins) {
		if (plugins != null) {
			this.plugins = plugins;

			ConcurrentHashMap<String, PluginInstanceInfo> groupPluginInfoHashMap = new ConcurrentHashMap<>();
			ConcurrentHashMap<String, PluginInstanceInfo> apiPluginInfoHashMap = new ConcurrentHashMap<>();
			ConcurrentHashMap<String, PluginInfo> gatewayPluginInfoHashMap = new ConcurrentHashMap<>();

			for (PluginInstanceInfo pluginInstanceInfo : plugins) {

				if (PluginScopeType.GROUP.getScopeType().equalsIgnoreCase(pluginInstanceInfo.getScopeType())) {
					groupPluginInfoHashMap.put(pluginInstanceInfo.getScopeValue(), pluginInstanceInfo);
				}
				else if (PluginScopeType.API.getScopeType().equalsIgnoreCase(pluginInstanceInfo.getScopeType())) {
					apiPluginInfoHashMap.put(pluginInstanceInfo.getScopeValue(), pluginInstanceInfo);
				}

				List<PluginDetail> gatewayPluginDetails = pluginInstanceInfo.getPluginDetails();
				for (PluginDetail gatewayPluginDetail : gatewayPluginDetails) {
					if (!gatewayPluginInfoHashMap.containsKey(gatewayPluginDetail.getId())) {
						PluginInfo gatewayPluginInfo = buildGatewayPluginInfo(gatewayPluginDetail);
						if (gatewayPluginInfo != null) {
							//初始化的时候就应该检查，避免运行时检查，在QPS高的时候，可以明显提升网关的性能
							gatewayPluginInfo.check();
							gatewayPluginInfoHashMap.put(gatewayPluginDetail.getId(), gatewayPluginInfo);
						}
					}
				}

			}

			this.groupPluginInfoMap = groupPluginInfoHashMap;
			this.apiPluginInfoMap = apiPluginInfoHashMap;
			this.gatewayPluginInfoMap = gatewayPluginInfoHashMap;
		}
	}

	public void refreshGroupRoute(Map<String, GroupContext> groups) {

		ConcurrentHashMap<String, Map<String, GroupContext.ContextRoute>> newGroupPathRouteMap = new ConcurrentHashMap<>();
		ConcurrentHashMap<String, Map<String, GroupContext.ContextRoute>> newGroupWildcardPathRouteMap = new ConcurrentHashMap<>();

		ConcurrentHashMap<String, Map<String, GroupContext.ContextRoute>> newGroupUnitPathRouteMap = new ConcurrentHashMap<>();
		ConcurrentHashMap<String, Map<String, GroupContext.ContextRoute>> newGroupUnitWildcardPathRouteMap = new ConcurrentHashMap<>();

		if (groups != null) {
			for (Map.Entry<String, GroupContext> entry : groups.entrySet()) {
				GroupContext groupContext = entry.getValue();
				Map<String, GroupContext.ContextRoute> newGroupPathRoute = new HashMap<>();
				Map<String, GroupContext.ContextRoute> newGroupWildcardPathRoute = new HashMap<>();

				Map<String, GroupContext.ContextRoute> newGroupUnitPathRoute = new HashMap<>();
				Map<String, GroupContext.ContextRoute> newGroupUnitWildcardPathRoute = new HashMap<>();

				for (GroupContext.ContextRoute route : groupContext.getRoutes()) {
					String path = route.getPath();
					// convert path parameter to group wildcard path
					if (path.contains("{") && path.contains("}") || path.contains("*") || path.contains("?")) {
						newGroupWildcardPathRoute.put(buildPathKey(groupContext, route), route);
						newGroupUnitWildcardPathRoute.put(buildUnitPathKey(groupContext, route), route);
					}
					else {
						newGroupPathRoute.put(buildPathKey(groupContext, route), route);
						newGroupUnitPathRoute.put(buildUnitPathKey(groupContext, route), route);
						if (StringUtils.isNotEmpty(route.getPathMapping())) {
							newGroupPathRoute.put(buildPathMappingKey(groupContext, route), route);
							newGroupUnitPathRoute.put(buildUnitPathKey(groupContext, route), route);
						}
					}
				}
				newGroupWildcardPathRouteMap.put(entry.getKey(), newGroupWildcardPathRoute);
				newGroupPathRouteMap.put(entry.getKey(), newGroupPathRoute);

				newGroupUnitWildcardPathRouteMap.put(entry.getKey(), newGroupUnitWildcardPathRoute);
				newGroupUnitPathRouteMap.put(entry.getKey(), newGroupUnitPathRoute);
			}
		}
		this.groupPathRouteMap = newGroupPathRouteMap;
		this.groupWildcardPathRouteMap = newGroupWildcardPathRouteMap;

		this.groupUnitPathRouteMap = newGroupUnitPathRouteMap;
		this.groupUnitWildcardPathRouteMap = newGroupUnitWildcardPathRouteMap;

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

	public GroupContext.ContextRoute getGroupUnitPathRoute(String group, String path) {
		Map<String, GroupContext.ContextRoute> groupPathRouteMap = this.groupUnitPathRouteMap.get(group);
		if (groupPathRouteMap != null && groupPathRouteMap.containsKey(path)) {
			return groupPathRouteMap.get(path);
		}

		Map<String, GroupContext.ContextRoute> groupWildcardPathRouteMap = this.groupUnitWildcardPathRouteMap.get(group);
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
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
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

	private String buildPathKey(GroupContext groupContext, GroupContext.ContextRoute route) {
		switch (groupContext.getPredicate().getApiType()) {
		case MS:
			return String.format("%s|/%s/%s%s", route.getMethod(), route.getNamespace(), route.getService(), route.getPath());
		case EXTERNAL:
		default:
			return String.format("%s|%s", route.getMethod(), route.getPath());
		}
	}

	private String buildUnitPathKey(GroupContext groupContext, GroupContext.ContextRoute route) {
		return String.format("%s|/%s%s", route.getMethod(), route.getService(), route.getPath());
	}

	private String buildPathMappingKey(GroupContext groupContext, GroupContext.ContextRoute route) {
		return String.format("%s|%s", route.getMethod(), route.getPathMapping());
	}

	protected PluginInfo buildGatewayPluginInfo(PluginDetail gatewayPluginDetail) {
		//不同插件类型，作不同方式处理
		PluginInfo pluginInfo = deserializePlugin(gatewayPluginDetail, gatewayPluginDetail.getPluginArgInfos());

		return pluginInfo;
	}

	private <T extends PluginInfo> T deserializePlugin(PluginInfo basePlugin, List<PluginArgInfo> pluginArgs) {
		if (basePlugin == null || pluginArgs == null) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_ERROR);
		}

		//查询网关插件参数信息，存储到map
		Map<String, Object> pluginArgMap = new HashMap<>(pluginArgs.size());
		pluginArgs.forEach((pluginArg -> pluginArgMap.put(pluginArg.getKey(), pluginArg.getValue())));
		//不同插件类型，作不同方式处理
		T pluginDetail;
		PluginType pluginType = PluginType.getPluginType(basePlugin.getType());
		if (pluginType == null) {
			return null;
		}
		try {
			//构建插件实例
			Class<? extends PluginInfo> gatewayPluginClazz = pluginType.getPluginClazz();
			pluginDetail = (T) gatewayPluginClazz.newInstance();
			//复制插件通用属性
			org.springframework.beans.BeanUtils.copyProperties(basePlugin, pluginDetail);
			//复制插件私有参数,map转为bean
			BeanUtils.populate(pluginDetail, pluginArgMap);
		}
		catch (Throwable t) {
			//日志打印出原始异常
			logger.error("build plugin error: plugin type is: {}", basePlugin.getType(), t);
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_ERROR, "构建" + basePlugin.getType() + "插件异常");
		}

		return pluginDetail;
	}

	public List<PluginDetail> getPluginDetails(String group, String apiId) {
		List<PluginDetail> pluginDetails = null;

		PluginInstanceInfo groupPluginInfo = groupPluginInfoMap.get(group);
		PluginInstanceInfo apiPluginInfo = apiPluginInfoMap.get(Optional.ofNullable(apiId).orElse(""));

		Stream<PluginDetail> pluginDetailStream = null;
		if (groupPluginInfo != null && apiPluginInfo != null) {
			pluginDetailStream = Stream.of(groupPluginInfo.getPluginDetails(), apiPluginInfo.getPluginDetails())
					.flatMap(Collection::stream);
		}
		else if (groupPluginInfo != null) {
			pluginDetailStream = groupPluginInfo.getPluginDetails().stream();
		}
		else if (apiPluginInfo != null) {
			pluginDetailStream = apiPluginInfo.getPluginDetails().stream();
		}

		// 去重、排序
		if (pluginDetailStream != null) {
			pluginDetails = PluginUtil.sortPluginDetail(pluginDetailStream);
		}

		return pluginDetails;
	}

	public PluginInfo getPluginInfo(String pluginId) {

		return gatewayPluginInfoMap.get(Optional.ofNullable(pluginId).orElse(""));
	}

}
