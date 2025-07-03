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

/**
 * 插件范围类型.
 * @author seanlxliu
 * @since 2019/9/10
 */
public enum PluginScopeType {
	/**
	 * 绑定组.
	 */
	GROUP("group"),

	/**
	 * 绑定API.
	 */
	API("api");

	private final String scopeType;

	PluginScopeType(String scopeType) {
		this.scopeType = scopeType;
	}

	public static PluginScopeType getScopeType(String scopeType) {
		for (PluginScopeType pluginScopeType : PluginScopeType.values()) {
			if (pluginScopeType.scopeType.equals(scopeType)) {
				return pluginScopeType;
			}
		}
		return null;
	}

	public String getScopeType() {
		return scopeType;
	}
}
