/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core.model;

public class UnitNamespace {

	private String id;

	private String name;

	private String businessSystemName;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBusinessSystemName() {
		return businessSystemName;
	}

	public void setBusinessSystemName(String businessSystemName) {
		this.businessSystemName = businessSystemName;
	}

	@Override
	public String toString() {
		return "UnitNamespace{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", businessSystemName='" + businessSystemName + '\'' +
				'}';
	}
}
