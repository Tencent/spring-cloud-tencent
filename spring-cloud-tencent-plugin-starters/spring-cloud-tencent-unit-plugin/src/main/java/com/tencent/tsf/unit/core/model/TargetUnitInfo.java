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
