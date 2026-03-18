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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.util.StringUtils;

/**
 * Utils for Jackson.
 *
 * @author Haotian Zhang, cheese8
 */
public final class JacksonUtils {

	/**
	 * Object Mapper.
	 */
	public static final JsonMapper OM = new JsonMapper();

	private static final Logger LOG = LoggerFactory.getLogger(JacksonUtils.class);

	static {
		OM.rebuild().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	private JacksonUtils() {
	}

	/**
	 * Object to Json.
	 * @param object object to be serialized
	 * @param <T> type of object
	 * @return Json String
	 */
	public static <T> String serialize2Json(T object) {
		return serialize2Json(object, false);
	}

	/**
	 * Object to Json.
	 * @param object object to be serialized
	 * @param pretty pretty print
	 * @param <T> type of object
	 * @return Json String
	 */
	public static <T> String serialize2Json(T object, boolean pretty) {
		try {
			if (pretty) {
				ObjectWriter objectWriter = OM.writerWithDefaultPrettyPrinter();
				return objectWriter.writeValueAsString(object);
			}
			else {
				return OM.writeValueAsString(object);
			}
		}
		catch (JacksonException e) {
			LOG.error("Object to Json failed. {}", object, e);
			throw new RuntimeException("Object to Json failed.", e);
		}
	}

	public static <T> T deserialize(String jsonStr, Class<T> type) {
		try {
			return OM.readValue(jsonStr, type);
		}
		catch (JacksonException e) {
			LOG.error("Json to object failed. {}", type, e);
			throw new RuntimeException("Json to object failed.", e);
		}
	}

	public static <T> T deserialize(String jsonStr, TypeReference<T> typeReference) {
		try {
			return OM.readValue(jsonStr, typeReference);
		}
		catch (JacksonException e) {
			LOG.error("Json to object failed. {}", typeReference, e);
			throw new RuntimeException("Json to object failed.", e);
		}
	}

	public static <T> List<T> deserializeCollection(String jsonArrayStr, Class<T> clazz) {
		JavaType javaType = getCollectionType(ArrayList.class, clazz);
		try {
			return  (List<T>) OM.readValue(jsonArrayStr, javaType);
		}
		catch (Exception t) {
			throw new RuntimeException(t);
		}
	}

	public static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
		return OM.getTypeFactory().constructParametricType(collectionClass, elementClasses);
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
		catch (JacksonException e) {
			LOG.error(
					"Json to map failed. check if the format of the json string[{}] is correct.", jsonStr, e);
			throw new RuntimeException("Json to map failed.", e);
		}
	}
}
