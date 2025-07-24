/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core;


import com.tencent.polaris.api.utils.StringUtils;

public enum TransformAlgorithmEnum {

	/**
	 * hash后取模.
	 */
	HASHCODE,
	/**
	 * 直接取模.
	 */
	MOD,
	/**
	 * 字符串截取.
	 */
	SUBSTR,
	/**
	 * 扩展用.
	 */
	OTHER;

	public static TransformAlgorithmEnum getTransformAlgorith(String type) {
		if (StringUtils.isEmpty(type)) {
			return OTHER;
		}
		for (TransformAlgorithmEnum transformAlgorithmEnum : TransformAlgorithmEnum.values()) {
			if (transformAlgorithmEnum.name().equalsIgnoreCase(type)) {
				return transformAlgorithmEnum;
			}
		}
		// 其他的当做 other
		return OTHER;
	}

}
