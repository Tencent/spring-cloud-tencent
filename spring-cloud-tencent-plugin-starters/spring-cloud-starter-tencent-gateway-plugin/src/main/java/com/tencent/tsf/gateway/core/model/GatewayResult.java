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

import java.io.Serializable;
import java.util.List;

/**
 * @author kysonli
 * 2019/4/10 11:14
 */
public class GatewayResult<T> implements Serializable {
	private static final long serialVersionUID = -8391900871963415134L;

	private String gatewayId;

	private String gatewayName;

	private String gatewayGroupId;

	private Integer reversion;

	private String updatedTime;

	private List<T> result;

	public String getGatewayId() {
		return gatewayId;
	}

	public void setGatewayId(String gatewayId) {
		this.gatewayId = gatewayId;
	}

	public String getGatewayName() {
		return gatewayName;
	}

	public void setGatewayName(String gatewayName) {
		this.gatewayName = gatewayName;
	}

	public String getGatewayGroupId() {
		return gatewayGroupId;
	}

	public void setGatewayGroupId(String gatewayGroupId) {
		this.gatewayGroupId = gatewayGroupId;
	}

	public Integer getReversion() {
		return reversion;
	}

	public void setReversion(Integer reversion) {
		this.reversion = reversion;
	}

	public String getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(String updatedTime) {
		this.updatedTime = updatedTime;
	}

	public List<T> getResult() {
		return result;
	}

	public void setResult(List<T> result) {
		this.result = result;
	}
}
