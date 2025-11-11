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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.polaris.plugins.configuration.connector.consul.ConsulConfigContext;
import com.tencent.tsf.gateway.core.constant.GatewayConstant;
import com.tencent.tsf.gateway.core.model.GatewayAllResult;
import com.tencent.tsf.gateway.core.model.GroupApiResult;
import com.tencent.tsf.gateway.core.model.GroupResult;
import com.tencent.tsf.gateway.core.model.PathRewriteResult;
import com.tencent.tsf.gateway.core.model.PathWildcardResult;
import com.tencent.tsf.gateway.core.model.PluginInstanceInfoResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shade.polaris.org.apache.commons.io.IOUtils;

public class GatewayConsulConfig {

	private static final Logger logger = LoggerFactory.getLogger(GatewayConsulConfig.class);
	private final String keyPrefix;
	private final String type;
	private final AtomicBoolean isFirstLoad = new AtomicBoolean(true);
	private final ConsulClient consulClient;
	private final ConsulConfigContext consulConfigContext;
	private final Runnable refreshAction;
	private Long index = -1L;
	private GatewayAllResult gatewayAllResult;

	public GatewayConsulConfig(String keyPrefix, String type,
			ConsulClient consulClient, ConsulConfigContext consulConfigContext, Runnable refreshAction) {
		this.keyPrefix = keyPrefix;
		this.type = type;
		this.consulClient = consulClient;
		this.consulConfigContext = consulConfigContext;
		this.refreshAction = refreshAction;
	}

	public String getType() {
		return type;
	}

	public GatewayAllResult getGatewayAllResult() {
		return gatewayAllResult;
	}

	public void firstLoad() {
		if (isFirstLoad.compareAndSet(true, false)) {
			watch(true);
		}
	}

	public void watch() {
		watch(false);
	}

	public void watch(boolean isFirstLoad) {
		try {
			Response<List<GetValue>> watchResponse = consulClient.getKVValues(keyPrefix,
					consulConfigContext.getAclToken(), new QueryParams(consulConfigContext.getWaitTime(), index));

			Long newIndex = watchResponse.getConsulIndex();
			if (logger.isDebugEnabled()) {
				logger.debug("[watch] keyPrefix:{}, index: {}, newIndex: {}, response is empty:{}", keyPrefix, index, newIndex, watchResponse.getValue() == null);
			}

			boolean change = false;
			if (newIndex != null && !Objects.equals(index, newIndex)) {
				change = true;
				index = newIndex;
			}

			if (watchResponse.getValue() == null) {
				// only load from the cache file when the first load and response is empty.
				if (isFirstLoad) {
					gatewayAllResult = loadResponseFromFile();
					logger.debug("[watch] loadResponseFromFile, keyPrefix: {}", keyPrefix);
				}
				return;
			}

			if (change) {
				gatewayAllResult = parseGroupResponse(watchResponse);
				if (!isFirstLoad) {
					refreshAction.run();
				}
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
	}

	private GatewayAllResult loadResponseFromFile() {
		GroupResult groupResult = null;
		GroupApiResult groupApiResult = new GroupApiResult();
		groupApiResult.setResult(new ArrayList<>());

		PathRewriteResult pathRewriteResult = new PathRewriteResult();
		PathWildcardResult pathWildcardResult = null;

		PluginInstanceInfoResult pluginInstanceInfoResult = new PluginInstanceInfoResult();
		pluginInstanceInfoResult.setResult(new ArrayList<>());

		switch (type) {
		case GatewayConstant.GROUP_FILE_NAME:
			groupResult = (GroupResult) readLocalRepo(GatewayConstant.GROUP_FILE_NAME, GroupResult.class);
			break;
		case GatewayConstant.API_FILE_NAME:
			groupApiResult = (GroupApiResult) readLocalRepo(GatewayConstant.API_FILE_NAME, GroupApiResult.class);
			break;
		case GatewayConstant.PATH_REWRITE_FILE_NAME:
			pathRewriteResult = (PathRewriteResult) readLocalRepo(GatewayConstant.PATH_REWRITE_FILE_NAME, PathRewriteResult.class);
			break;
		case GatewayConstant.PATH_WILDCARD_FILE_NAME:
			pathWildcardResult = (PathWildcardResult) readLocalRepo(GatewayConstant.PATH_WILDCARD_FILE_NAME, PathWildcardResult.class);
			break;
		case GatewayConstant.PLUGIN_FILE_NAME:
			pluginInstanceInfoResult = (PluginInstanceInfoResult) readLocalRepo(GatewayConstant.PLUGIN_FILE_NAME, PluginInstanceInfoResult.class);
			break;
		}

		return new GatewayAllResult(groupResult, groupApiResult, pathRewriteResult, pathWildcardResult, pluginInstanceInfoResult);
	}

	private GatewayAllResult parseGroupResponse(Response<List<GetValue>> listResponse) {
		GroupResult groupResult = null;
		GroupApiResult groupApiResult = new GroupApiResult();
		groupApiResult.setResult(new ArrayList<>());

		PathRewriteResult pathRewriteResult = new PathRewriteResult();
		PathWildcardResult pathWildcardResult = null;

		PluginInstanceInfoResult pluginInstanceInfoResult = new PluginInstanceInfoResult();
		pluginInstanceInfoResult.setResult(new ArrayList<>());


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
				GroupApiResult apiTemp = JacksonUtils.deserialize(getValue.getDecodedValue(), GroupApiResult.class);
				groupApiResult.getResult().addAll(apiTemp.getResult());
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
			case GatewayConstant.PLUGIN_FILE_NAME:
				if (logger.isDebugEnabled()) {
					logger.debug("[parseResponse] Received path plugin data: {}", getValue.getDecodedValue());
				}
				PluginInstanceInfoResult pluginTemp = JacksonUtils.deserialize(getValue.getDecodedValue(), PluginInstanceInfoResult.class);
				pluginInstanceInfoResult.getResult().addAll(pluginTemp.getResult());
				break;
			}

		}
		switch (type) {
		case GatewayConstant.GROUP_FILE_NAME:
			saveAsFile(JacksonUtils.serialize2Json(groupResult), GatewayConstant.GROUP_FILE_NAME);
			break;
		case GatewayConstant.API_FILE_NAME:
			saveAsFile(JacksonUtils.serialize2Json(groupApiResult), GatewayConstant.API_FILE_NAME);
			break;
		case GatewayConstant.PATH_REWRITE_FILE_NAME:
			saveAsFile(JacksonUtils.serialize2Json(pathRewriteResult), GatewayConstant.PATH_REWRITE_FILE_NAME);
			break;
		case GatewayConstant.PATH_WILDCARD_FILE_NAME:
			saveAsFile(JacksonUtils.serialize2Json(pathWildcardResult), GatewayConstant.PATH_WILDCARD_FILE_NAME);
			break;
		case GatewayConstant.PLUGIN_FILE_NAME:
			saveAsFile(JacksonUtils.serialize2Json(pluginInstanceInfoResult), GatewayConstant.PLUGIN_FILE_NAME);
			break;
		}

		return new GatewayAllResult(groupResult, groupApiResult, pathRewriteResult, pathWildcardResult, pluginInstanceInfoResult);
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
			return JacksonUtils.deserialize(new String(bytes, StandardCharsets.UTF_8), repoResultClazz);
		}
		catch (Throwable t) {
			logger.warn("[readLocalRepo] json serialize data to group occur exception: {}", t.getMessage());
			return null;
		}
	}
}
