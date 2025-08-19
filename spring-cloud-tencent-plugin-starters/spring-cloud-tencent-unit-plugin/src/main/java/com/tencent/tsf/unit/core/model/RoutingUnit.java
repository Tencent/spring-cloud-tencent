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
 * RoutingUnit 根据客户要素计算得到的目标单元相关信息，包括计算中间态的数据.
 */
public class RoutingUnit {

	// 客户号
	private String customerNumber;

	// 分片KEY，分片号信息
	private String shardingKey;

	// 匹配的目标单元
	private UnitRouteInfo.MatchRoute matchRoute;

	public RoutingUnit() {
	}

	public RoutingUnit(String customerNumber, String shardingKey, UnitRouteInfo.MatchRoute matchRoute) {
		this.customerNumber = customerNumber;
		this.shardingKey = shardingKey;
		this.matchRoute = matchRoute;
	}

	public String getCustomerNumber() {
		return customerNumber;
	}

	public void setCustomerNumber(String customerNumber) {
		this.customerNumber = customerNumber;
	}

	public String getShardingKey() {
		return shardingKey;
	}

	public void setShardingKey(String shardingKey) {
		this.shardingKey = shardingKey;
	}

	public UnitRouteInfo.MatchRoute getMatchRoute() {
		return matchRoute;
	}

	public void setMatchRoute(UnitRouteInfo.MatchRoute matchRoute) {
		this.matchRoute = matchRoute;
	}

	public String getUnitId() {
		if (matchRoute == null) {
			return null;
		}

		return matchRoute.getActualUnitId();
	}
}
