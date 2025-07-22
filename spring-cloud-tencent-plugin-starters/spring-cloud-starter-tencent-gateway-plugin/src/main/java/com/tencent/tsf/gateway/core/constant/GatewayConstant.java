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
 * @author kysonli
 * 2019/4/10 15:34
 */
public final class GatewayConstant {
	/**
	 * CONTEXT_ROUTE.
	 */
	public static final String CONTEXT_ROUTE = "contextRoute";
	/**
	 * 网关能够访问的API分组的缓存文件名称.
	 */
	public static final String GROUP_FILE_NAME = "group";
	/**
	 * 网关能够被访问的API的缓存文件名称.
	 */
	public static final String API_FILE_NAME = "api";
	/**
	 * 网关分组绑定的插件缓存文件名称.
	 */
	public static final String PLUGIN_FILE_NAME = "plugin";
	/**
	 * 网关分组绑定的插件缓存文件名称.
	 */
	public static final String PATH_REWRITE_FILE_NAME = "rewrite";
	/**
	 * PATH_REWRITE_EXCEPTION.
	 */
	public static final String PATH_REWRITE_EXCEPTION = "PathRewriteException";
	/**
	 * 网关路径通配规则缓存文件名称.
	 */
	public static final String PATH_WILDCARD_FILE_NAME = "wildcard";
	/**
	 * 本地文件的文件扩展名称.
	 */
	public static final String FILE_SUFFIX = ".json";
	/**
	 * 网关插件通用部署组名称.
	 */
	public static final String GATEWAY_COMMON_DEPLOY_GROUP_ID = "common";
	/**
	 * 在Spring Cloud Gateway中用来全量匹配所有请求的.
	 */
	public static final String GATEWAY_WILDCARD_SERVICE_NAME = "wildcard_tsf_gateway";
	/**
	 * Tsf Gateway 的限流标签.
	 */
	public static final String TSF_GATEWAY_RATELIMIT_CONTEXT_TAG = "tsf-gateway-ratelimit-context";
	/**
	 * 本地文件的仓库跟目录.
	 */
	private static final String GATEWAY_REPO_ROOT = System.getProperty("user.home");
	/**
	 * Gateway 本地仓库的文件目录.
	 */
	public static final String GATEWAY_REPO_PREFIX = GATEWAY_REPO_ROOT + "/tsf/gateway/";
	/**
	 * NON_UNIT_TYPE.
	 */
	public static final String NON_UNIT_TYPE = "non-unit";
	/**
	 * NON_UNIT_TRANSFER_TYPE.
	 */
	public static final String NON_UNIT_TRANSFER_TYPE = "non-unit";
	/**
	 * UNIT_TYPE.
	 */
	public static final String UNIT_TYPE = "unit";
	/**
	 * UNIT_TRANSFER_TYPE.
	 */
	public static final String UNIT_TRANSFER_TYPE = "ms_unit_proxy";

	private GatewayConstant() {

	}

}
