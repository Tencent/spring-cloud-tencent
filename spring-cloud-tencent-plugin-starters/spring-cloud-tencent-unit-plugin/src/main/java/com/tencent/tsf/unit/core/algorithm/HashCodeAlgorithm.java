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

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.unit.core.TransformAlgorithmEnum;

public class HashCodeAlgorithm implements IUnitTransformAlgorithm {

	private final Map<String, Object> options;

	private final int mod;

	public HashCodeAlgorithm(Map<String, Object> options) {
		this.options = options;
		mod = Integer.parseInt(String.valueOf(options.get("mod")));
	}

	@Override
	public String getName() {
		return TransformAlgorithmEnum.HASHCODE.name();
	}

	@Override
	public String transform(String cid) {
		if (StringUtils.isEmpty(cid)) {
			return null;
		}

		return String.valueOf(cid.hashCode() % mod);
	}
}
