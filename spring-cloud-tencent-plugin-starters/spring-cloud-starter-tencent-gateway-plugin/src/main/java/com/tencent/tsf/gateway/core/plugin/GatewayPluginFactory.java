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

package com.tencent.tsf.gateway.core.plugin;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @ClassName GatewayPluginContext
 * @Description TODO
 * @Author vmershen
 * @Date 2019/7/9 15:44
 * @Version 1.0
 */
public class GatewayPluginFactory {

	//根据插件type找对应的插件执行类
	private static final Map<String, IGatewayPlugin> gatewayPluginExecutorMap = new HashMap<>();
	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	//获取插件业务执行器
	public static IGatewayPlugin getGatewayPluginExecutor(String type) {
		if (StringUtils.isEmpty(type)) {
			return null;
		}
		return gatewayPluginExecutorMap.get(type);
	}

	public void putGatewayPlugin(String type, IGatewayPlugin gatewayPlugin) {
		gatewayPluginExecutorMap.put(type, gatewayPlugin);
	}

	public void close() {
		logger.info("gatewayPluginExecutorMap start clear");
		gatewayPluginExecutorMap.clear();
		logger.info("gatewayPluginExecutorMap clear success");
	}

}
