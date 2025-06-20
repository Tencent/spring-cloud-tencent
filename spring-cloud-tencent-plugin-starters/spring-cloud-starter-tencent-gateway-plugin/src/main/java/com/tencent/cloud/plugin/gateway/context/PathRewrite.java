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

package com.tencent.cloud.plugin.gateway.context;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author seanlxliu
 * @date 2020/5/14
 */
public class PathRewrite {

	/**
	 * 路径重写规则ID.
	 */
	private String pathRewriteId;

	/**
	 * 网关部署组ID.
	 */
	private String gatewayGroupId;

	/**
	 * 正则表达式.
	 */
	private String regex;

	/**
	 * 替换的内容.
	 */
	private String replacement;

	/**
	 * 是否屏蔽映射后路径，Y: 是 N: 否.
	 */
	private String blocked;

	/**
	 * 规则顺序，越小优先级越高.
	 */
	private Integer order;

	/**
	 * 路径重写规则IDs.
	 */
	@JsonIgnore
	private List<String> pathRewriteIds;

	public String getPathRewriteId() {
		return pathRewriteId;
	}

	public void setPathRewriteId(String pathRewriteId) {
		this.pathRewriteId = pathRewriteId;
	}

	public String getGatewayGroupId() {
		return gatewayGroupId;
	}

	public void setGatewayGroupId(String gatewayGroupId) {
		this.gatewayGroupId = gatewayGroupId;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public String getReplacement() {
		return replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	public String getBlocked() {
		return blocked;
	}

	public void setBlocked(String blocked) {
		this.blocked = blocked;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public List<String> getPathRewriteIds() {
		return pathRewriteIds;
	}

	public void setPathRewriteIds(List<String> pathRewriteIds) {
		this.pathRewriteIds = pathRewriteIds;
	}

	@Override
	public String toString() {
		return "PathRewrite{" +
				"pathRewriteId='" + pathRewriteId + '\'' +
				", gatewayGroupId='" + gatewayGroupId + '\'' +
				", regex='" + regex + '\'' +
				", replacement='" + replacement + '\'' +
				", blocked='" + blocked + '\'' +
				", order=" + order +
				'}';
	}
}
