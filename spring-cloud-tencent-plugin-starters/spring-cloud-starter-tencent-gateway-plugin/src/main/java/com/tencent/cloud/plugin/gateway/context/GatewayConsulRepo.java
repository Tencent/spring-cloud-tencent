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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.plugins.configuration.connector.consul.ConsulConfigConstants;
import com.tencent.polaris.plugins.configuration.connector.consul.ConsulConfigContext;
import com.tencent.tsf.gateway.core.constant.AuthMode;
import com.tencent.tsf.gateway.core.constant.GatewayConstant;
import com.tencent.tsf.gateway.core.model.GatewayAllResult;
import com.tencent.tsf.gateway.core.model.GatewayResult;
import com.tencent.tsf.gateway.core.model.Group;
import com.tencent.tsf.gateway.core.model.GroupApi;
import com.tencent.tsf.gateway.core.model.GroupApiResult;
import com.tencent.tsf.gateway.core.model.GroupResult;
import com.tencent.tsf.gateway.core.model.GroupSecret;
import com.tencent.tsf.gateway.core.model.PathRewriteResult;
import com.tencent.tsf.gateway.core.model.PathWildcardResult;
import com.tencent.tsf.gateway.core.model.PathWildcardRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationEventPublisher;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;

public class GatewayConsulRepo {

	private static final Logger logger = LoggerFactory.getLogger(GatewayConsulRepo.class);

	private final ContextGatewayProperties contextGatewayProperties;

	private final ContextGatewayPropertiesManager contextGatewayPropertiesManager;

	private final ApplicationEventPublisher publisher;

	private GatewayConsulConfig groupConfig;

	private GatewayConsulConfig apiConfig;

	private GatewayConsulConfig rewriteConfig;

	private GatewayConsulConfig wildcardConfig;

	private GatewayConsulConfig pluginConfig;

	private ConsulClient consulClient;

	private ConsulConfigContext consulConfigContext;

	private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5, new NamedThreadFactory("consul-gateway-watch", true));

	public GatewayConsulRepo(ContextGatewayProperties contextGatewayProperties,
			PolarisSDKContextManager polarisSDKContextManager,
			ContextGatewayPropertiesManager contextGatewayPropertiesManager,
			ApplicationEventPublisher publisher, String tsfGroupId) {
		this.contextGatewayProperties = contextGatewayProperties;
		this.contextGatewayPropertiesManager = contextGatewayPropertiesManager;
		this.publisher = publisher;

		List<ServerConnectorConfigImpl> serverConnectorConfigs = polarisSDKContextManager.getSDKContext().getConfig()
				.getGlobal().getServerConnectors();
		if (serverConnectorConfigs == null || serverConnectorConfigs.size() != 1 || !SERVER_CONNECTOR_CONSUL.equals(serverConnectorConfigs.get(0)
				.getProtocol())) {
			logger.warn("GatewayConsulRepo not enable, serverConnectorConfigs:{}", serverConnectorConfigs);
			return;
		}

		// init consul client
		ServerConnectorConfigImpl connectorConfig = serverConnectorConfigs.get(0);

		if (CollectionUtils.isEmpty(connectorConfig.getAddresses())) {
			logger.warn("GatewayConsulRepo not enable, connectorConfig:{}", connectorConfig);
			return;
		}

		String address = connectorConfig.getAddresses().get(0);
		int lastIndex = address.lastIndexOf(":");
		String agentHost = address.substring(0, lastIndex);
		int agentPort = Integer.parseInt(address.substring(lastIndex + 1));
		logger.info("Connect to consul config server : [{}].", address);

		consulClient = new ConsulClient(new ConsulRawClient(agentHost, agentPort));
		initConsulConfigContext(connectorConfig);

		groupConfig = new GatewayConsulConfig("tsf_gateway/" + tsfGroupId + "/" + GatewayConstant.GROUP_FILE_NAME + "/data",
				GatewayConstant.GROUP_FILE_NAME, consulClient, consulConfigContext, this::refreshAndPublishGroupConfig);
		apiConfig = new GatewayConsulConfig("tsf_gateway/" + tsfGroupId + "/" + GatewayConstant.API_FILE_NAME + "/data",
				GatewayConstant.API_FILE_NAME, consulClient, consulConfigContext, this::refreshAndPublishGroupConfig);
		rewriteConfig = new GatewayConsulConfig("tsf_gateway/" + tsfGroupId + "/" + GatewayConstant.PATH_REWRITE_FILE_NAME + "/data",
				GatewayConstant.PATH_REWRITE_FILE_NAME, consulClient, consulConfigContext, this::refreshAndPublishGroupConfig);
		wildcardConfig = new GatewayConsulConfig("tsf_gateway/" + tsfGroupId + "/" + GatewayConstant.PATH_WILDCARD_FILE_NAME + "/data",
				GatewayConstant.PATH_WILDCARD_FILE_NAME, consulClient, consulConfigContext, this::refreshAndPublishGroupConfig);
		pluginConfig = new GatewayConsulConfig("tsf_gateway/common/plugin/data",
				GatewayConstant.PLUGIN_FILE_NAME, consulClient, consulConfigContext, this::refreshPlugin);

		groupConfig.firstLoad();
		apiConfig.firstLoad();
		rewriteConfig.firstLoad();
		wildcardConfig.firstLoad();
		pluginConfig.firstLoad();

		mergeAndRefreshGroupConfig();
		refreshPlugin();

		scheduledExecutorService.scheduleAtFixedRate(groupConfig::watch, consulConfigContext.getDelay(), consulConfigContext.getDelay(), TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(apiConfig::watch, consulConfigContext.getDelay(), consulConfigContext.getDelay(), TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(rewriteConfig::watch, consulConfigContext.getDelay(), consulConfigContext.getDelay(), TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(wildcardConfig::watch, consulConfigContext.getDelay(), consulConfigContext.getDelay(), TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(pluginConfig::watch, consulConfigContext.getDelay(), consulConfigContext.getDelay(), TimeUnit.MILLISECONDS);
	}

	private void mergeAndRefreshGroupConfig() {
		GatewayAllResult result = new GatewayAllResult(
				groupConfig.getGatewayAllResult().getGroupResult(),
				apiConfig.getGatewayAllResult().getGroupApiResult(),
				rewriteConfig.getGatewayAllResult().getPathRewriteResult(),
				wildcardConfig.getGatewayAllResult().getPathWildcardResult(),
				pluginConfig.getGatewayAllResult().getPluginInstanceInfoResult());
		refreshGatewayGroupConfig(result);
	}

	private void refreshPlugin() {
		contextGatewayProperties.setPlugins(Optional.of(pluginConfig.getGatewayAllResult()).
				map(GatewayAllResult::getPluginInstanceInfoResult).map(GatewayResult::getResult).orElse(Collections.emptyList()));
		contextGatewayPropertiesManager.refreshPlugins(contextGatewayProperties.getPlugins());
	}

	private void refreshAndPublishGroupConfig() {
		mergeAndRefreshGroupConfig();
		this.publisher.publishEvent(new RefreshRoutesEvent(this));
	}

	private void initConsulConfigContext(ServerConnectorConfigImpl connectorConfig) {
		// init consul config context.
		consulConfigContext = new ConsulConfigContext();
		// token
		String tokenStr = connectorConfig.getToken();
		if (StringUtils.isNotBlank(tokenStr)) {
			consulConfigContext.setAclToken(tokenStr);
		}

		Map<String, String> metadata = connectorConfig.getMetadata();
		if (CollectionUtils.isNotEmpty(metadata)) {
			String waitTimeStr = metadata.get(ConsulConfigConstants.WAIT_TIME_KEY);
			if (StringUtils.isNotBlank(waitTimeStr)) {
				try {
					int waitTime = Integer.parseInt(waitTimeStr);
					consulConfigContext.setWaitTime(waitTime);
				}
				catch (Exception e) {
					logger.warn("wait time string {} is not integer.", waitTimeStr, e);
				}
			}

			String delayStr = metadata.get(ConsulConfigConstants.DELAY_KEY);
			if (StringUtils.isNotBlank(delayStr)) {
				try {
					int delay = Integer.parseInt(delayStr);
					consulConfigContext.setDelay(delay);
				}
				catch (Exception e) {
					logger.warn("delay string {} is not integer.", delayStr, e);
				}
			}

			String consulErrorSleepStr = metadata.get(ConsulConfigConstants.CONSUL_ERROR_SLEEP_KEY);
			if (StringUtils.isNotBlank(consulErrorSleepStr)) {
				try {
					long consulErrorSleep = Long.parseLong(consulErrorSleepStr);
					consulConfigContext.setConsulErrorSleep(consulErrorSleep);
				}
				catch (Exception e) {
					logger.warn("delay string {} is not integer.", consulErrorSleepStr, e);
				}
			}
		}
	}

	private void refreshGatewayGroupConfig(GatewayAllResult gatewayAllResult) {
		GroupResult groupResult = gatewayAllResult.getGroupResult();
		GroupApiResult groupApiResult = gatewayAllResult.getGroupApiResult();
		PathRewriteResult pathRewriteResult = gatewayAllResult.getPathRewriteResult();
		PathWildcardResult pathWildcardResult = gatewayAllResult.getPathWildcardResult();

		Map<String, RouteDefinition> routes = new HashMap<>();
		Map<String, GroupContext> groups = new HashMap<>();

		if (groupResult != null && groupResult.getResult() != null) {
			for (Group group : groupResult.getResult()) {
				routes.put(group.getGroupId(), getRouteDefinition(group));

				GroupContext.ContextPredicate contextPredicate = new GroupContext.ContextPredicate();
				contextPredicate.setApiType(ApiType.valueOf(group.getGroupType().toUpperCase(Locale.ROOT)));
				contextPredicate.setContext(group.getGroupContext());
				contextPredicate.setNamespace(new GroupContext.ContextNamespace(
						Position.valueOf(group.getNamespaceNameKeyPosition()
								.toUpperCase(Locale.ROOT)), group.getNamespaceNameKey()));
				contextPredicate.setService(new GroupContext.ContextService(
						Position.valueOf(group.getServiceNameKeyPosition()
								.toUpperCase(Locale.ROOT)), group.getServiceNameKey()));

				List<GroupContext.ContextSecret> secrets = new ArrayList<>();
				if (CollectionUtils.isNotEmpty(group.getSecretList())) {
					for (GroupSecret groupSecret : group.getSecretList()) {
						GroupContext.ContextSecret contextSecret = new GroupContext.ContextSecret();
						contextSecret.setName(groupSecret.getSecretName());
						contextSecret.setId(groupSecret.getSecretId());
						contextSecret.setKey(groupSecret.getSecretKey());
						contextSecret.setStatus(groupSecret.getStatus());
						contextSecret.setExpiredTime(groupSecret.getExpiredTime());

						secrets.add(contextSecret);
					}
				}

				GroupContext.ContextAuth auth = new GroupContext.ContextAuth();
				auth.setType(AuthMode.getMode(group.getAuthMode()));
				auth.setSecrets(secrets);


				GroupContext groupContext = new GroupContext();
				groupContext.setRoutes(new ArrayList<>());
				groupContext.setPredicate(contextPredicate);
				groupContext.setAuth(auth);

				groups.put(group.getGroupId(), groupContext);
			}
		}

		if (groupApiResult != null) {
			for (GroupApi groupApi : groupApiResult.getResult()) {
				GroupContext groupContext = groups.get(groupApi.getGroupId());
				if (groupContext == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("group api {} not found in group {}", groupApi.getApiId(), groupApi.getGroupId());
					}
					continue;
				}

				GroupContext.ContextRoute contextRoute = new GroupContext.ContextRoute();
				contextRoute.setApiId(groupApi.getApiId());
				contextRoute.setHost(groupApi.getHost());
				contextRoute.setPath(groupApi.getPath());
				contextRoute.setPathMapping(groupApi.getPathMapping());
				contextRoute.setMethod(groupApi.getMethod());
				contextRoute.setService(groupApi.getServiceName());
				contextRoute.setNamespaceId(groupApi.getNamespaceId());
				contextRoute.setNamespace(groupApi.getNamespaceName());
				if (groupApi.getTimeout() != null) {
					Map<String, String> metadata = new HashMap<>();
					metadata.put("response-timeout", String.valueOf(groupApi.getTimeout()));
					contextRoute.setMetadata(metadata);
				}
				groupContext.getRoutes().add(contextRoute);
			}
		}

		if (pathWildcardResult != null && pathWildcardResult.getResult() != null) {
			for (PathWildcardRule wildcardRule : pathWildcardResult.getResult()) {

				GroupContext.ContextRoute contextRoute = new GroupContext.ContextRoute();
				contextRoute.setPath(wildcardRule.getWildCardPath());
				contextRoute.setMethod(wildcardRule.getMethod());
				contextRoute.setService(wildcardRule.getServiceName());
				contextRoute.setNamespaceId(wildcardRule.getNamespaceId());
				contextRoute.setNamespace(wildcardRule.getNamespaceName());
				if (wildcardRule.getTimeout() != null) {
					Map<String, String> metadata = new HashMap<>();
					metadata.put("response-timeout", String.valueOf(wildcardRule.getTimeout()));
					contextRoute.setMetadata(metadata);
				}

				GroupContext groupContext = groups.get(wildcardRule.getGroupId());
				if (groupContext != null && groupContext.getRoutes() != null) {
					groupContext.getRoutes().add(contextRoute);
				}
				else {
					logger.warn("path wildcard rule {} not found in group {}", wildcardRule.getWildCardId(), wildcardRule.getGroupId());
				}
			}
		}

		contextGatewayProperties.setGroups(groups);
		contextGatewayProperties.setRoutes(routes);
		contextGatewayProperties.setPathRewrites(Optional.ofNullable(pathRewriteResult).map(PathRewriteResult::getResult)
				.orElse(new ArrayList<>()));
		if (logger.isDebugEnabled()) {
			logger.debug("Gateway config loaded. :{}", JacksonUtils.serialize2Json(contextGatewayProperties));
		}

		contextGatewayPropertiesManager.setPathRewrites(contextGatewayProperties.getPathRewrites());


		contextGatewayPropertiesManager.refreshGroupRoute(contextGatewayProperties.getGroups());
	}



	private RouteDefinition getRouteDefinition(Group group) {
		RouteDefinition routeDefinition = new RouteDefinition();
		routeDefinition.setUri(URI.create("lb://" + group.getGroupId()));

		PredicateDefinition contextPredicateDefinition = new PredicateDefinition();
		contextPredicateDefinition.setName("Context");
		contextPredicateDefinition.setArgs(Collections.singletonMap("group", group.getGroupId()));
		PredicateDefinition pathPredicateDefinition = new PredicateDefinition();
		pathPredicateDefinition.setName("Path");
		pathPredicateDefinition.setArgs(Collections.singletonMap("pattern", group.getGroupContext() + "/**"));
		routeDefinition.setPredicates(Arrays.asList(contextPredicateDefinition, pathPredicateDefinition));

		FilterDefinition contextFilterDefinition = new FilterDefinition();
		contextFilterDefinition.setName("Context");
		contextFilterDefinition.setArgs(Collections.singletonMap("group", group.getGroupId()));
		routeDefinition.setFilters(Collections.singletonList(contextFilterDefinition));

		routeDefinition.setOrder(-1);

		return routeDefinition;
	}

}
