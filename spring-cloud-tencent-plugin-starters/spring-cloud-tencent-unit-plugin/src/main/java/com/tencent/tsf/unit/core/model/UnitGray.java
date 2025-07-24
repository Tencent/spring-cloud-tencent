/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
