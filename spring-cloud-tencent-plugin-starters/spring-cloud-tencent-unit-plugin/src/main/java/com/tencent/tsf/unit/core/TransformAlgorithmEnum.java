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
