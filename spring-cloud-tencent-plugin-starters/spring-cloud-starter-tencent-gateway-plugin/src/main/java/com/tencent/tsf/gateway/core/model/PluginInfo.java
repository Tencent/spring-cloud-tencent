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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.gateway.core.exception.TsfGatewayError;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;

public class PluginInfo implements Serializable {

	private static final long serialVersionUID = -4823276300296184640L;
	//网关插件id
	private String id;

	//插件名称
	private String name;

	//插件类型
	private String type;

	//插件执行顺序
	private Integer order;

	//插件描述
	private String description;

	private String createdTime;

	private String updatedTime;


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	@JsonIgnore
	public void check() {
		if (StringUtils.isEmpty(name)) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "插件名称参数错误");
		}
		if (StringUtils.isEmpty(type)) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "插件类型参数错误");
		}
	}

	@Override
	public String toString() {
		return "PluginInfo{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", type='" + type + '\'' +
				", order=" + order +
				", description='" + description + '\'' +
				", createdTime='" + createdTime + '\'' +
				", updatedTime='" + updatedTime + '\'' +
				'}';
	}
}
