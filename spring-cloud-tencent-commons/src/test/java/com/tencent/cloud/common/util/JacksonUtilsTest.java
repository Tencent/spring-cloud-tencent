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

package com.tencent.cloud.common.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.tsf.core.entity.Tag;
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
	public void testDeserialize() {
		Tag originalTag = new Tag("k1", "v1");
		Tag deserializedTag = JacksonUtils.deserialize("{\"k\":\"k1\",\"v\":\"v1\"}", Tag.class);
		assertThat(deserializedTag).isEqualTo(originalTag);
		assertThat(deserializedTag.hashCode()).isEqualTo(originalTag.hashCode());
		assertThat(deserializedTag.toString()).isEqualTo(originalTag.toString());

		deserializedTag = JacksonUtils.deserialize("{\"k\":\"k2\",\"v\":\"v2\"}", new TypeReference<Tag>() { });
		assertThat(deserializedTag.getKey()).isEqualTo("k2");
		assertThat(deserializedTag.getValue()).isEqualTo("v2");

		assertThatThrownBy(() -> JacksonUtils.deserialize("{", Tag.class))
				.isInstanceOf(RuntimeException.class).hasMessage("Json to object failed.");
		assertThatThrownBy(() -> JacksonUtils.deserialize("{", new TypeReference<Tag>() { }))
				.isInstanceOf(RuntimeException.class).hasMessage("Json to object failed.");
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

	@Test
	public void testDeserializeCollection() {
		String jsonStr = "[{\"k\":\"k1\",\"v\":\"v1\"},{\"k\":\"k2\",\"v\":\"v2\"}]";
		List<Tag> list = JacksonUtils.deserializeCollection(jsonStr, Tag.class);
		assertThat(list).isNotNull();
		assertThat(list.size()).isEqualTo(2);
		assertThat(list.get(0).getKey()).isEqualTo("k1");
		assertThat(list.get(0).getValue()).isEqualTo("v1");
		assertThat(list.get(1).getKey()).isEqualTo("k2");
		assertThat(list.get(1).getValue()).isEqualTo("v2");

		assertThatThrownBy(() -> JacksonUtils.deserializeCollection("{", Tag.class))
				.isExactlyInstanceOf(RuntimeException.class);
	}
}
