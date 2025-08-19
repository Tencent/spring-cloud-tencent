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

public class UnitGray {

	private TencentUnitGray tencent;

	public TencentUnitGray getTencent() {
		return tencent;
	}

	public void setTencent(TencentUnitGray tencent) {
		this.tencent = tencent;
	}

	public static class TencentUnitGray {
		private UnitGrayList unitGrayList;

		public UnitGrayList getUnitGrayList() {
			return unitGrayList;
		}

		public void setUnitGrayList(UnitGrayList unitGrayList) {
			this.unitGrayList = unitGrayList;
		}
	}

	public static class UnitGrayList {
		private String version;

		private List<String> ids;

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public List<String> getIds() {
			return ids;
		}

		public void setIds(List<String> ids) {
			this.ids = ids;
		}
	}
}
