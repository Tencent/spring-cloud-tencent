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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
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
import com.tencent.tsf.gateway.core.model.Group;
import com.tencent.tsf.gateway.core.model.GroupApi;
import com.tencent.tsf.gateway.core.model.GroupApiResult;
import com.tencent.tsf.gateway.core.model.GroupResult;
import com.tencent.tsf.gateway.core.model.GroupSecret;
import com.tencent.tsf.gateway.core.model.PathRewriteResult;
import com.tencent.tsf.gateway.core.model.PathWildcardResult;
import com.tencent.tsf.gateway.core.model.PathWildcardRule;
import com.tencent.tsf.gateway.core.model.PluginInstanceInfoResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shade.polaris.org.apache.commons.io.IOUtils;

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
	private final AtomicLong gatewayGroupIndex = new AtomicLong(-1);
	private final AtomicLong commonPluginIndex = new AtomicLong(-1);
	private ConsulClient consulClient;
	private ConsulConfigContext consulConfigContext;
	private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2, new NamedThreadFactory("consul-gateway-watch", true));

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

		Response<List<GetValue>> listResponse = consulClient.getKVValues("tsf_gateway/" + tsfGroupId, consulConfigContext.getAclToken());

		gatewayGroupIndex.set(listResponse.getConsulIndex());
		if (listResponse.getValue() != null) {
			refreshGatewayGroupConfig(parseGroupResponse(listResponse));
		}
		else {
			logger.info("try to load gateway group config from local file.");
			refreshGatewayGroupConfig(loadResponseFromFile());
		}

		scheduledExecutorService.scheduleAtFixedRate(() -> {
			try {
				Response<List<GetValue>> watchResponse = consulClient.getKVValues("tsf_gateway/" + tsfGroupId,
						consulConfigContext.getAclToken(), new QueryParams(consulConfigContext.getWaitTime(), gatewayGroupIndex.get()));
				// 404
				if (watchResponse.getValue() == null) {
					return;
				}
				// 200
				Long newIndex = watchResponse.getConsulIndex();
				if (logger.isDebugEnabled()) {
					logger.debug("[watch group] index: {}, newIndex: {}", gatewayGroupIndex.get(), newIndex);
				}
				if (newIndex != null && !Objects.equals(gatewayGroupIndex.get(), newIndex)) {
					gatewayGroupIndex.set(newIndex);
					refreshGatewayGroupConfig(parseGroupResponse(watchResponse));
					this.publisher.publishEvent(new RefreshRoutesEvent(this));
				}

			}
			catch (Exception e) {
				logger.warn("Gateway config watch error.", e);
				try {
					Thread.sleep(consulConfigContext.getConsulErrorSleep());
				}
				catch (Exception ex) {
					logger.error("error in sleep, msg: " + e.getMessage());
				}
			}

		}, consulConfigContext.getDelay(), consulConfigContext.getDelay(), TimeUnit.MILLISECONDS);


		scheduledExecutorService.scheduleAtFixedRate(() -> {
			try {
				Response<List<GetValue>> watchResponse = consulClient.getKVValues("tsf_gateway/common/plugin",
						consulConfigContext.getAclToken(), new QueryParams(consulConfigContext.getWaitTime(), commonPluginIndex.get()));
				// 404
				if (watchResponse.getValue() == null) {
					return;
				}
				// 200
				Long newIndex = watchResponse.getConsulIndex();
				if (logger.isDebugEnabled()) {
					logger.debug("[watch plugin] index: {}, newIndex: {}", commonPluginIndex.get(), newIndex);
				}
				if (newIndex != null && !Objects.equals(commonPluginIndex.get(), newIndex)) {
					commonPluginIndex.set(listResponse.getConsulIndex());
					parsePluginResponse(watchResponse);
				}
			}
			catch (Exception e) {
				logger.error("Gateway plugin watch error.", e);
				try {
					Thread.sleep(consulConfigContext.getConsulErrorSleep());
				}
				catch (Exception ex) {
					logger.error("error in sleep, msg: " + e.getMessage());
				}
			}
		}, consulConfigContext.getDelay(), consulConfigContext.getDelay(), TimeUnit.MILLISECONDS);

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

	private void parsePluginResponse(Response<List<GetValue>> listResponse) {

		PluginInstanceInfoResult pluginInstanceInfoResult = new PluginInstanceInfoResult();
		pluginInstanceInfoResult.setResult(new ArrayList<>());

		for (GetValue getValue : listResponse.getValue()) {
			if (logger.isDebugEnabled()) {
				logger.debug("[parseResponse] Received plugin data: {}", getValue.getDecodedValue());
			}
			PluginInstanceInfoResult temp = JacksonUtils.deserialize(getValue.getDecodedValue(), PluginInstanceInfoResult.class);
			pluginInstanceInfoResult.getResult().addAll(temp.getResult());
		}

		saveAsFile(JacksonUtils.serialize2Json(pluginInstanceInfoResult), GatewayConstant.PLUGIN_FILE_NAME);

		contextGatewayProperties.setPlugins(pluginInstanceInfoResult.getResult());
		contextGatewayPropertiesManager.refreshPlugins(contextGatewayProperties.getPlugins());

	}

	private GatewayAllResult loadResponseFromFile() {
		GroupResult groupResult = (GroupResult) readLocalRepo(GatewayConstant.GROUP_FILE_NAME, GroupResult.class);
		GroupApiResult groupApiResult = (GroupApiResult) readLocalRepo(GatewayConstant.API_FILE_NAME, GroupApiResult.class);
		PathRewriteResult pathRewriteResult = (PathRewriteResult) readLocalRepo(GatewayConstant.PATH_REWRITE_FILE_NAME, PathRewriteResult.class);
		PathWildcardResult pathWildcardResult = (PathWildcardResult) readLocalRepo(GatewayConstant.PATH_WILDCARD_FILE_NAME, PathWildcardResult.class);
		return new GatewayAllResult(groupResult, groupApiResult, pathRewriteResult, pathWildcardResult);
	}

	private Object readLocalRepo(String type, Class<?> repoResultClazz) {
		byte[] bytes;
		try (FileInputStream fin = new FileInputStream(getRepoStoreFile(type)); InputStreamReader isr = new InputStreamReader(fin)) {
			bytes = IOUtils.toByteArray(isr, "utf-8");
			if (bytes == null || bytes.length == 0) {
				return null;
			}
		}
		catch (IOException t) {
			logger.warn("[readLocalRepo] read group info from file occur exception: {}", t.getMessage());
			return null;
		}
		try {
			return JacksonUtils.deserialize(new String(bytes, "utf-8"), repoResultClazz);
		}
		catch (Throwable t) {
			logger.warn("[readLocalRepo] json serialize data to group occur exception: {}", t.getMessage());
			return null;
		}
	}

	private GatewayAllResult parseGroupResponse(Response<List<GetValue>> listResponse) {
		GroupResult groupResult = null;
		GroupApiResult groupApiResult = new GroupApiResult();
		groupApiResult.setResult(new ArrayList<>());

		PathRewriteResult pathRewriteResult = new PathRewriteResult();
		PathWildcardResult pathWildcardResult = null;


		for (GetValue getValue : listResponse.getValue()) {
			String key = getValue.getKey();
			String[] keySplit = key.split("/");
			// format example: tsf_gateway/group-xxx/group/data
			if (keySplit.length < 4) {
				continue;
			}
			switch (keySplit[2]) {
			case GatewayConstant.GROUP_FILE_NAME:
				if (logger.isDebugEnabled()) {
					logger.debug("[parseResponse] Received group data: {}", getValue.getDecodedValue());
				}
				groupResult = JacksonUtils.deserialize(getValue.getDecodedValue(), GroupResult.class);
				break;
			case GatewayConstant.API_FILE_NAME:
				if (logger.isDebugEnabled()) {
					logger.debug("[parseResponse] Received api data: {}", getValue.getDecodedValue());
				}
				GroupApiResult temp = JacksonUtils.deserialize(getValue.getDecodedValue(), GroupApiResult.class);
				groupApiResult.getResult().addAll(temp.getResult());
				break;
			case GatewayConstant.PATH_REWRITE_FILE_NAME:
				if (logger.isDebugEnabled()) {
					logger.debug("[parseResponse] Received path rewrite data: {}", getValue.getDecodedValue());
				}
				pathRewriteResult = JacksonUtils.deserialize(getValue.getDecodedValue(), PathRewriteResult.class);
				break;
			case GatewayConstant.PATH_WILDCARD_FILE_NAME:
				if (logger.isDebugEnabled()) {
					logger.debug("[parseResponse] Received path wildcard data: {}", getValue.getDecodedValue());
				}
				pathWildcardResult = JacksonUtils.deserialize(getValue.getDecodedValue(), PathWildcardResult.class);
				break;
			}

		}

		saveAsFile(JacksonUtils.serialize2Json(groupResult), GatewayConstant.GROUP_FILE_NAME);
		saveAsFile(JacksonUtils.serialize2Json(groupApiResult), GatewayConstant.API_FILE_NAME);
		saveAsFile(JacksonUtils.serialize2Json(pathRewriteResult), GatewayConstant.PATH_REWRITE_FILE_NAME);
		saveAsFile(JacksonUtils.serialize2Json(pathWildcardResult), GatewayConstant.PATH_WILDCARD_FILE_NAME);

		return new GatewayAllResult(groupResult, groupApiResult, pathRewriteResult, pathWildcardResult);
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
				groupContext.getRoutes().add(contextRoute);
			}
		}

		contextGatewayProperties.setGroups(groups);
		contextGatewayProperties.setRoutes(routes);
		contextGatewayProperties.setPathRewrites(Optional.ofNullable(pathRewriteResult).map(PathRewriteResult::getResult)
				.orElse(new ArrayList<>()));

		logger.debug("Gateway config loaded. :{}", JacksonUtils.serialize2Json(contextGatewayProperties));

		contextGatewayPropertiesManager.setPathRewrites(contextGatewayProperties.getPathRewrites());


		contextGatewayPropertiesManager.refreshGroupRoute(contextGatewayProperties.getGroups());
	}

	private void saveAsFile(String data, String type) {
		try {
			// 写入文件
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(getRepoStoreFile(type)));
			writer.write(data);
			writer.close();
		}
		catch (Throwable t) {
			logger.warn("[tsf-gateway] save as file occur exception.", t);
		}
	}

	private File getRepoStoreFile(String type) {
		String filePath = GatewayConstant.GATEWAY_REPO_PREFIX + type + GatewayConstant.FILE_SUFFIX;
		File file = new File(filePath);
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
		}
		catch (IOException e) {

			logger.warn("[tsf-gateway] load group info from local file occur error. filePath: " + filePath, e);
		}
		return file;
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
