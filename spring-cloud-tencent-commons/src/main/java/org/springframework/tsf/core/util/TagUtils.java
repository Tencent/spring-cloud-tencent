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

package org.springframework.tsf.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.common.util.TsfTagUtils;

import org.springframework.tsf.core.entity.Metadata;
import org.springframework.tsf.core.entity.Tag;

/**
 * Compatible with old versions TSF SDK.
 *
 * @author Shedfree Wu
 */
public final class TagUtils {

	private TagUtils() {
	}

	public static <T> String serializeToJson(T object) {
		return JacksonUtils.serialize2Json(object);
	}

	public static List<Tag> deserializeTagList(String buffer) {
		return TsfTagUtils.deserializeTagList(buffer);
	}

	public static Metadata deserializeMetadata(String buffer) {
		return TsfTagUtils.deserializeMetadata(buffer);
	}

	public static List<Tag> getTagListFromTagMap(Map<String, Tag> tagMap) {
		return new ArrayList<>(tagMap.values());
	}

	public static Map<String, Tag> buildTagMapFromTagList(List<Tag> tagList) {
		Map<String, Tag> result = new HashMap<>();
		for (Tag tag : tagList) {
			result.put(tag.getKey(), tag);
		}
		return result;
	}
}
