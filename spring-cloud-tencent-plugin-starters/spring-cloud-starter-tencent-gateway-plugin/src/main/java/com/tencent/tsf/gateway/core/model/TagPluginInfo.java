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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.cloud.plugin.gateway.context.Position;

/**
 * @author seanlxliu
 * @since 2019/9/12
 */
public class TagPluginInfo {

	/**
	 * 参数位置.
	 */
	private Position tagPosition;

	/**
	 * 参数名称.
	 */
	private String preTagName;

	/**
	 * 转化后标签名称，不填写表示使用原参数名称.
	 */
	private String postTagName;

	/**
	 * 是否设置为TraceId：N/Y，默认为N.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String traceIdEnabled;

	public Position getTagPosition() {
		return tagPosition;
	}

	public void setTagPosition(Position tagPosition) {
		this.tagPosition = tagPosition;
	}

	public String getPreTagName() {
		return preTagName;
	}

	public void setPreTagName(String preTagName) {
		this.preTagName = preTagName;
	}

	public String getPostTagName() {
		return postTagName;
	}

	public void setPostTagName(String postTagName) {
		this.postTagName = postTagName;
	}

	public String getTraceIdEnabled() {
		return traceIdEnabled;
	}

	public void setTraceIdEnabled(String traceIdEnabled) {
		this.traceIdEnabled = traceIdEnabled;
	}
}
