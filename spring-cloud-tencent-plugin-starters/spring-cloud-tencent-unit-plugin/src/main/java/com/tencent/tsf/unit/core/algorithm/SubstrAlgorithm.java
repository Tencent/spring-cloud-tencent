/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core.algorithm;

import java.util.Map;

import com.tencent.tsf.unit.core.TransformAlgorithmEnum;

public class SubstrAlgorithm implements IUnitTransformAlgorithm {

	private final Map<String, Object> options;

	public SubstrAlgorithm(Map<String, Object> options) {
		this.options = options;
	}

	@Override
	public String getName() {
		return TransformAlgorithmEnum.SUBSTR.name();
	}

	@Override
	public String transform(String cid) {
		return cid;
	}
}
