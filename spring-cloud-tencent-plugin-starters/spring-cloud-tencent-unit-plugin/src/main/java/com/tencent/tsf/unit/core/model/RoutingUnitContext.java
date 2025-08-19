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

package com.tencent.tsf.unit.core.model;

/**
 * 路由单元信息上下文，场景：服务A -> 服务B，在服务B中获取路由单元上下文.
 */
public class RoutingUnitContext extends RoutingUnit {

	private String unitId;

	public RoutingUnitContext(String customerNumber, String shardingKey, String unitId) {
		setCustomerNumber(customerNumber);
		setShardingKey(shardingKey);
		this.unitId = unitId;
	}

	@Override
	public String getUnitId() {
		return unitId;
	}

	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}
}
