/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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
 *
 */

package com.tencent.cloud.common.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * test for {@link JacksonUtils}
 *@author lepdou 2022-05-27
 */
@RunWith(MockitoJUnitRunner.class)
public class JacksonUtilsTest {

	@Test
	public void test() {
		Map<String, String> sourceMap = new HashMap<>();
		sourceMap.put("k1", "v1");
		sourceMap.put("k2", "v2");
		sourceMap.put("k3", "v3");

		Map<String, String> map = JacksonUtils.deserialize2Map(JacksonUtils.serialize2Json(sourceMap));

		Assert.assertEquals(sourceMap.size(), map.size());
		Assert.assertEquals(sourceMap.get("k1"), map.get("k1"));
		Assert.assertEquals(sourceMap.get("k2"), map.get("k2"));
		Assert.assertEquals(sourceMap.get("k3"), map.get("k3"));
	}
}
