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
import com.tencent.tsf.unit.core.exception.ErrorCode;
import com.tencent.tsf.unit.core.exception.TencentUnitException;

public class ModAlgorithm implements IUnitTransformAlgorithm {

	private final Map<String, Object> options;

	private final int mod;

	public ModAlgorithm(Map<String, Object> options) {
		this.options = options;
		mod = Integer.parseInt(String.valueOf(options.get("mod")));
	}

	@Override
	public String getName() {
		return TransformAlgorithmEnum.MOD.name();
	}

	@Override
	public String transform(String cid) {
		if (StringUtils.isEmpty(cid)) {
			return null;
		}
		try {
			long id = Long.parseLong(cid);
			return String.valueOf(id % mod);
		}
		catch (Exception e) {
			throw new TencentUnitException(ErrorCode.CUSTOMER_NUMBER_TRANSFORM_ERROR, "format invalid, customNumber:" + cid, e);
		}
	}
}
