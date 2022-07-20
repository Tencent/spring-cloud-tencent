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

package com.tencent.cloud.common.util.expresstion;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import org.springframework.util.CollectionUtils;

/**
 * the utils for parse label expression.
 *
 * @author lepdou 2022-05-13
 * @author cheese8 2022-06-20
 */
public final class ExpressionLabelUtils {

	/**
	 * the prefix of expression.
	 */
	public static final String LABEL_PREFIX = "${";
	/**
	 * the expression prefix of header label.
	 */
	public static final String LABEL_HEADER_PREFIX = "${http.header.";
	/**
	 * the length of expression header label prefix.
	 */
	public static final int LABEL_HEADER_PREFIX_LEN = LABEL_HEADER_PREFIX.length();
	/**
	 * the expression prefix of query.
	 */
	public static final String LABEL_QUERY_PREFIX = "${http.query.";
	/**
	 * the length of expression query label prefix.
	 */
	public static final int LABEL_QUERY_PREFIX_LEN = LABEL_QUERY_PREFIX.length();
	/**
	 * the expression prefix of cookie.
	 */
	public static final String LABEL_COOKIE_PREFIX = "${http.cookie.";
	/**
	 * the length of expression cookie label prefix.
	 */
	public static final int LABEL_COOKIE_PREFIX_LEN = LABEL_COOKIE_PREFIX.length();
	/**
	 * the expression of method.
	 */
	public static final String LABEL_METHOD = "${http.method}";
	/**
	 * the expression of uri.
	 */
	public static final String LABEL_URI = "${http.uri}";
	/**
	 * the suffix of expression.
	 */
	public static final String LABEL_SUFFIX = "}";

	private ExpressionLabelUtils() {
	}

	public static boolean isExpressionLabel(String labelKey) {
		if (StringUtils.isEmpty(labelKey)) {
			return false;
		}
		return StringUtils.startsWith(labelKey, LABEL_PREFIX) && StringUtils.endsWith(labelKey, LABEL_SUFFIX);
	}

	public static String parseHeaderKey(String expression) {
		return expression.substring(LABEL_HEADER_PREFIX_LEN, expression.length() - 1);
	}

	public static String parseQueryKey(String expression) {
		return expression.substring(LABEL_QUERY_PREFIX_LEN, expression.length() - 1);
	}

	public static String parseCookieKey(String expression) {
		return expression.substring(LABEL_COOKIE_PREFIX_LEN, expression.length() - 1);
	}

	public static String getQueryValue(String queryString, String queryKey) {
		if (StringUtils.isBlank(queryString)) {
			return StringUtils.EMPTY;
		}
		String[] queries = StringUtils.split(queryString, "&");
		if (queries == null || queries.length == 0) {
			return StringUtils.EMPTY;
		}
		for (String query : queries) {
			String[] queryKV = StringUtils.split(query, "=");
			if (queryKV != null && queryKV.length == 2 && StringUtils.equals(queryKV[0], queryKey)) {
				return queryKV[1];
			}
		}
		return StringUtils.EMPTY;
	}

	public static String getFirstValue(Map<String, Collection<String>> valueMaps, String key) {
		if (CollectionUtils.isEmpty(valueMaps)) {
			return StringUtils.EMPTY;
		}

		Collection<String> values = valueMaps.get(key);

		if (CollectionUtils.isEmpty(values)) {
			return StringUtils.EMPTY;
		}

		for (String value : values) {
			return value;
		}

		return StringUtils.EMPTY;
	}
}
