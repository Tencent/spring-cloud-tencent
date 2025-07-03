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

package com.tencent.tsf.gateway.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.gateway.core.constant.PluginConstants;
import com.tencent.tsf.gateway.core.exception.TsfGatewayError;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import com.tencent.tsf.gateway.core.util.PluginUtil;

/**
 * @author seanlxliu
 * @since 2019/9/12
 */
public class TagPlugin extends PluginInfo {

	private static final long serialVersionUID = -2682243185036956532L;

	/**
	 * 参数配置的JSON串.
	 */
	private String tagPluginInfoList;

	public String getTagPluginInfoList() {
		return tagPluginInfoList;
	}

	public void setTagPluginInfoList(String tagPluginInfoList) {
		this.tagPluginInfoList = tagPluginInfoList;
	}

	@Override
	@JsonIgnore
	public void check() {
		super.check();
		if (StringUtils.isEmpty(tagPluginInfoList)) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "验证Tag插件参数");
		}

		if (StringUtils.length(tagPluginInfoList) > PluginConstants.TAG_PLUGIN_INFO_LIST_LIMIT ||
				!PluginUtil.predicateJsonFormat(tagPluginInfoList)) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_INVALID, "验证Tag插件参数");
		}
	}
}
