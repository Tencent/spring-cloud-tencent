/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core.model;


import com.fasterxml.jackson.annotation.JsonCreator;

public enum UnitTagPosition {

	/**
	 * HTTP HEADER.
	 */
	HEADER,
	/**
	 * HTTP QUERY.
	 */
	QUERY,
	/**
	 * HTTP COOKIE.
	 */
	COOKIE,
	/**
	 * HTTP PATH.
	 */
	PATH,
	/**
	 * TSF TAG.
	 */
	TSF_TAG;

	@JsonCreator
	public static UnitTagPosition fromString(String key) {
		for (UnitTagPosition tagPosition : UnitTagPosition.values()) {
			if (tagPosition.name().equalsIgnoreCase(key)) {
				return tagPosition;
			}
		}
		return null;
	}

}
