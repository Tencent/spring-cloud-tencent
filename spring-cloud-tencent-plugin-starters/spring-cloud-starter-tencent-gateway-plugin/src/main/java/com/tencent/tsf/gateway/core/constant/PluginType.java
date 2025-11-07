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

package com.tencent.tsf.gateway.core.constant;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import com.tencent.tsf.gateway.core.model.JwtPlugin;
import com.tencent.tsf.gateway.core.model.OAuthPlugin;
import com.tencent.tsf.gateway.core.model.PluginInfo;
import com.tencent.tsf.gateway.core.model.RequestTransformerPlugin;
import com.tencent.tsf.gateway.core.model.TagPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum PluginType {
	/**
	 * ReqTransformer 插件实现.
	 */
	REQ_TRANSFORMER("ReqTransformer", RequestTransformerPlugin.class),

	/**
	 * OAuth 插件实现.
	 */
	OAUTH("OAuth", OAuthPlugin.class),
	/**
	 * Jwt 插件实现.
	 */
	JWT("Jwt", JwtPlugin.class),
	/**
	 * Tag 转化插件实现.
	 */
	TAG("Tag", TagPlugin.class);

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final Map<String, String> pluginMap = new HashMap<>();

	static {
		pluginMap.put(OAUTH.name(), OAUTH.type);
		pluginMap.put(JWT.name(), JWT.type);
		pluginMap.put(TAG.name(), TAG.type);
	}

	private String type;
	private Class<? extends PluginInfo> pluginClazz;

	PluginType(String type, Class<? extends PluginInfo> pluginClazz) {
		this.type = type;
		this.pluginClazz = pluginClazz;
	}

	public static PluginType getPluginType(String name) {
		for (PluginType pluginType : PluginType.values()) {
			if (pluginType.type.equalsIgnoreCase(name)) {
				return pluginType;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Unknown plugin type:{} exception, please upgrade your gateway sdk", name);
		}
		return null;
	}

	public static Map<String, String> toMap() {
		return pluginMap;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Class<? extends PluginInfo> getPluginClazz() {
		return pluginClazz;
	}

	public void setPluginClazz(Class<? extends PluginInfo> pluginClazz) {
		this.pluginClazz = pluginClazz;
	}

}

