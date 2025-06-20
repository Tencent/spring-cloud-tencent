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

package com.tencent.tsf.gateway.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.gateway.core.exception.TsfGatewayError;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;

public class RequestTransformerPlugin extends PluginInfo {

	private static final long serialVersionUID = -2682243185036956532L;

	/**
	 * 参数配置的JSON串.
	 */
	private String pluginInfo;

	@JsonIgnore
	private RequestTransformerPluginInfo requestTransformerPluginInfo;

	public String getPluginInfo() {
		return pluginInfo;
	}

	public void setPluginInfo(String pluginInfo) {
		this.pluginInfo = pluginInfo;
	}

	public RequestTransformerPluginInfo getRequestTransformerPluginInfo() {
		return requestTransformerPluginInfo;
	}

	public void setRequestTransformerPluginInfo(
			RequestTransformerPluginInfo requestTransformerPluginInfo) {
		this.requestTransformerPluginInfo = requestTransformerPluginInfo;
	}

	@Override
	@JsonIgnore
	public void check() {
		super.check();
		if (StringUtils.isEmpty(pluginInfo)) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "验证插件参数");
		}

		try {
			requestTransformerPluginInfo = JacksonUtils.deserialize(pluginInfo, new TypeReference<RequestTransformerPluginInfo>() { });

		}
		catch (Throwable t) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "验证插件格式");
		}

		int sum = 0;
		for (TransformerAction action : requestTransformerPluginInfo.getActions()) {
			if (action.getWeight() == null || action.getWeight() < 0) {
				throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED,
						"权重值不合法:" + action.getWeight());
			}
			sum += action.getWeight();
		}
		if (sum > 100) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED,
					"验证插件权重失败，当前权重总和为" + sum);
		}
	}
}
