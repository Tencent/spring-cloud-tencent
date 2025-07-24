/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core.model;

import com.tencent.tsf.unit.core.exception.ErrorCode;
import com.tencent.tsf.unit.core.exception.TencentUnitException;

/**
 * 由业务系统+客户号计算出的目标单元信息，utils包使用的class.
 * 包括ns信息等
 */
public class TargetUnitInfo extends UnitInfo {
	// 逻辑分片ID，多个逻辑分片ID，映射到一个单元号里
	private String shardingKey;

	// namespace
	private String namespaceId;
	private String namespaceName;

	public TargetUnitInfo(UnitInfo unitInfo) {
		if (unitInfo == null) {
			throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, "unit info is invalid(null)");
		}

		this.setUnitId(unitInfo.getUnitId());
		this.setZoneId(unitInfo.getZoneId());
		this.setZoneName(unitInfo.getZoneName());
		this.setRegionId(unitInfo.getRegionId());
		this.setRegionName(unitInfo.getRegionName());
		this.setUnitType(unitInfo.getUnitType());
		this.setAllNamespaceList(unitInfo.getAllNamespaceList());
		this.setAllShardingKeyList(unitInfo.getAllShardingKeyList());
	}

	public String getShardingKey() {
		return shardingKey;
	}

	public void setShardingKey(String shardingKey) {
		this.shardingKey = shardingKey;
	}

	public String getNamespaceId() {
		return namespaceId;
	}

	public void setNamespaceId(String namespaceId) {
		this.namespaceId = namespaceId;
	}

	public String getNamespaceName() {
		return namespaceName;
	}

	public void setNamespaceName(String namespaceName) {
		this.namespaceName = namespaceName;
	}
}
