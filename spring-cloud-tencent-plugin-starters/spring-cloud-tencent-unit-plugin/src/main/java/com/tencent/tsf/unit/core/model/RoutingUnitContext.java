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
