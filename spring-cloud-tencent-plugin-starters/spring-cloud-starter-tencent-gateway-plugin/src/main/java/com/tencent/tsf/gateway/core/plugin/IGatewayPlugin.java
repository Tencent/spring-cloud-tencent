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

package com.tencent.tsf.gateway.core.plugin;

import com.tencent.tsf.gateway.core.TsfGatewayRequest;
import com.tencent.tsf.gateway.core.model.PluginInfo;
import com.tencent.tsf.gateway.core.model.PluginPayload;

/**
 * @author vmershen
 * 2019/7/7 17:08
 */
public interface IGatewayPlugin<T extends PluginInfo> {

	default PluginPayload invoke(PluginInfo info, TsfGatewayRequest tsfGatewayRequest) {
		return startUp((T) info, tsfGatewayRequest);
	}

	PluginPayload startUp(T pluginInfo, TsfGatewayRequest tsfGatewayRequest);
}
