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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link JacksonUtils}.
 *
 * @author lepdou, Haotian Zhang, cheese8
 */
@ExtendWith(MockitoExtension.class)
public class JacksonUtilsTest {

	@Test
	public void testSerialize2Json() {
		Map<String, String> sourceMap = new HashMap<>();
		sourceMap.put("k1", "v1");
		sourceMap.put("k2", "v2");
		sourceMap.put("k3", "v3");
		assertThat(JacksonUtils.serialize2Json(sourceMap)).isEqualTo("{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"}");
		assertThat(StringUtils.trimAllWhitespace(JacksonUtils.serialize2Json(sourceMap, true))).isEqualTo("{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"}");
	}

	@Test
	public void testDeserialize2Map() {
		String jsonStr = "{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"}";
		Map<String, String> map = JacksonUtils.deserialize2Map(jsonStr);
		assertThat(map.size()).isEqualTo(3);
		assertThat(map.get("k1")).isEqualTo("v1");
		assertThat(map.get("k2")).isEqualTo("v2");
		assertThat(map.get("k3")).isEqualTo("v3");
	}

	@Test
	public void testDeserializeBlankIntoEmptyMap() {
		Map<String, String> map = JacksonUtils.deserialize2Map("");
		assertThat(map).isNotNull();
		assertThat(map).isEmpty();
	}

	@Test
	public void testDeserializeThrowsRuntimeException() {
		String jsonStr = "{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"";
		assertThatThrownBy(() -> JacksonUtils.deserialize2Map(jsonStr))
				.isExactlyInstanceOf(RuntimeException.class).hasMessage("Json to map failed.");
	}
}
