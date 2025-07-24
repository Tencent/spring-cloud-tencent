/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
