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

import com.tencent.tsf.gateway.core.TsfGatewayRequest;
import com.tencent.tsf.gateway.core.model.PluginPayload;
import com.tencent.tsf.gateway.core.model.RequestTransformerPlugin;
import com.tencent.tsf.gateway.core.util.PluginUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReqTransformerGatewayPlugin implements IGatewayPlugin<RequestTransformerPlugin> {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public PluginPayload startUp(RequestTransformerPlugin plugin, TsfGatewayRequest tsfGatewayRequest) {
		PluginPayload payload = new PluginPayload();
		return PluginUtil.doRequestTransformer(plugin.getRequestTransformerPluginInfo(), tsfGatewayRequest, payload);
	}

}
