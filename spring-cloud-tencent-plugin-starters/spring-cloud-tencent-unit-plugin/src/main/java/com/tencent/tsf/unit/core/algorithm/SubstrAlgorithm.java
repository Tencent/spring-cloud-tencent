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
