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

import java.util.List;

/**
 * 单元信息，包括SDU或GDU，提供给utils使用的class.
 */
public class UnitInfo {
	// 单元号
	private String unitId;

	// 单元类型  SDU or GDU
	private String unitType;

	// region
	private String regionId;
	private String regionName;

	// zone
	private String zoneId;
	private String zoneName;

	// 当前单元对应的命名空间列表
	private List<UnitNamespace> allNamespaceList;

	// 当前单元对应的所有shardingId集合
	private List<Integer> allShardingKeyList;

	public String getUnitId() {
		return unitId;
	}

	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}

	public String getUnitType() {
		return unitType;
	}

	public void setUnitType(String unitType) {
		this.unitType = unitType;
	}

	public String getZoneId() {
		return zoneId;
	}

	public void setZoneId(String zoneId) {
		this.zoneId = zoneId;
	}

	public String getRegionId() {
		return regionId;
	}

	public void setRegionId(String regionId) {
		this.regionId = regionId;
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public String getZoneName() {
		return zoneName;
	}

	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}

	public List<UnitNamespace> getAllNamespaceList() {
		return allNamespaceList;
	}

	public void setAllNamespaceList(List<UnitNamespace> allNamespaceList) {
		this.allNamespaceList = allNamespaceList;
	}

	public List<Integer> getAllShardingKeyList() {
		return allShardingKeyList;
	}

	public void setAllShardingKeyList(List<Integer> allShardingKeyList) {
		this.allShardingKeyList = allShardingKeyList;
	}

	@Override
	public String toString() {
		return "UnitInfo{" +
				"unitId='" + unitId + '\'' +
				", unitType='" + unitType + '\'' +
				", regionId='" + regionId + '\'' +
				", regionName='" + regionName + '\'' +
				", zoneId='" + zoneId + '\'' +
				", zoneName='" + zoneName + '\'' +
				", allNamespaceList=" + allNamespaceList +
				", allShardingKeyList=" + allShardingKeyList +
				'}';
	}
}
