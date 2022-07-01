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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for {@link JacksonUtils}.
 *
 * @author lepdou, Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class JacksonUtilsTest {

	@Test
	public void testSerialize2Json() {
		Map<String, String> sourceMap = new HashMap<>();
		sourceMap.put("k1", "v1");
		sourceMap.put("k2", "v2");
		sourceMap.put("k3", "v3");
		assertEquals(JacksonUtils.serialize2Json(sourceMap), "{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"}");
	}

	@Test
	public void testDeserialize2Map() {
		String jsonStr = "{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"}";
		Map<String, String> map = JacksonUtils.deserialize2Map(jsonStr);
		assertEquals(map.size(), 3);
		assertEquals(map.get("k1"), "v1");
		assertEquals(map.get("k2"), "v2");
		assertEquals(map.get("k3"), "v3");
	}

	@Test
	public void testDeserializeBlankIntoEmptyMap() {
		assertTrue(JacksonUtils.deserialize2Map("").isEmpty());
	}

	@Test
	public void testDeserializeThrowsRuntimeException() {
		String jsonStr = "{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"";
		assertThrows(RuntimeException.class, () -> JacksonUtils.deserialize2Map(jsonStr), "Json to map failed.");
	}
}
