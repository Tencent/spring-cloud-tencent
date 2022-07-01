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
 */

package com.tencent.cloud.common.util;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;

/**
 * Utils for Jackson.
 *
 * @author Haotian Zhang
 */
public final class JacksonUtils {

	/**
	 * Object Mapper.
	 */
	public static final ObjectMapper OM = new ObjectMapper();

	private static final Logger LOG = LoggerFactory.getLogger(JacksonUtils.class);

	private JacksonUtils() {

	}

	/**
	 * Object to Json.
	 * @param object object to be serialized
	 * @param <T> type of object
	 * @return Json String
	 */
	public static <T> String serialize2Json(T object) {
		try {
			return OM.writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			LOG.error("Object to Json failed. {}", object, e);
			throw new RuntimeException("Object to Json failed.", e);
		}
	}

	/**
	 * Json to Map.
	 * @param jsonStr Json String
	 * @return Map
	 */
	public static Map<String, String> deserialize2Map(String jsonStr) {
		try {
			if (StringUtils.hasText(jsonStr)) {
				Map<String, Object> temp = OM.readValue(jsonStr, Map.class);
				Map<String, String> result = new HashMap<>();
				temp.forEach((key, value) -> {
					result.put(String.valueOf(key), String.valueOf(value));
				});
				return result;
			}
			return new HashMap<>();
		}
		catch (JsonProcessingException e) {
			LOG.error("Json to map failed. check if the format of the json string[{}] is correct.", jsonStr, e);
			throw new RuntimeException("Json to map failed.", e);
		}
	}

}
