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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.lang.StringUtils;

import org.springframework.util.CollectionUtils;

/**
 * the utils for parse label expression.
 *
 * @author lepdou 2022-05-13
 * @author cheese8 2022-06-20
 */
public final class ExpressionLabelUtils {

	private static final List<ExpressionParser> EXPRESSION_PARSERS;

	static {
		EXPRESSION_PARSERS = new ArrayList<>(2);
		EXPRESSION_PARSERS.add(new ExpressionParserV1());
		EXPRESSION_PARSERS.add(new ExpressionParserV2());
	}

	private ExpressionLabelUtils() {
	}

	public static boolean isExpressionLabel(String expression) {
		for (ExpressionParser parser : EXPRESSION_PARSERS) {
			if (parser.isExpressionLabel(expression)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isHeaderLabel(String expression) {
		for (ExpressionParser parser : EXPRESSION_PARSERS) {
			if (parser.isHeaderLabel(expression)) {
				return true;
			}
		}
		return false;
	}

	public static String parseHeaderKey(String expression) {
		for (ExpressionParser parser : EXPRESSION_PARSERS) {
			if (parser.isHeaderLabel(expression)) {
				return parser.parseHeaderKey(expression);
			}
		}
		return "";
	}

	public static boolean isQueryLabel(String expression) {
		for (ExpressionParser parser : EXPRESSION_PARSERS) {
			if (parser.isQueryLabel(expression)) {
				return true;
			}
		}
		return false;
	}

	public static String parseQueryKey(String expression) {
		for (ExpressionParser parser : EXPRESSION_PARSERS) {
			if (parser.isQueryLabel(expression)) {
				return parser.parseQueryKey(expression);
			}
		}
		return "";
	}

	public static boolean isCookieLabel(String expression) {
		for (ExpressionParser parser : EXPRESSION_PARSERS) {
			if (parser.isCookieLabel(expression)) {
				return true;
			}
		}
		return false;
	}

	public static String parseCookieKey(String expression) {
		for (ExpressionParser parser : EXPRESSION_PARSERS) {
			if (parser.isCookieLabel(expression)) {
				return parser.parseCookieKey(expression);
			}
		}
		return "";
	}

	public static boolean isMethodLabel(String expression) {
		for (ExpressionParser parser : EXPRESSION_PARSERS) {
			if (parser.isMethodLabel(expression)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isUriLabel(String expression) {
		for (ExpressionParser parser : EXPRESSION_PARSERS) {
			if (parser.isUriLabel(expression)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isCallerIPLabel(String expression) {
		for (ExpressionParser parser : EXPRESSION_PARSERS) {
			if (parser.isCallerIPLabel(expression)) {
				return true;
			}
		}
		return false;
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

	public static String getCookieFirstValue(Map<String, Collection<String>> valueMaps, String key) {
		if (CollectionUtils.isEmpty(valueMaps)) {
			return StringUtils.EMPTY;
		}

		Collection<String> values = valueMaps.get(HttpHeaderNames.COOKIE.toString());

		if (CollectionUtils.isEmpty(values)) {
			return StringUtils.EMPTY;
		}

		for (String value : values) {
			String[] cookieArray = StringUtils.split(value, ";");
			for (String cookieValue : cookieArray) {
				String[] cookieKV = StringUtils.split(cookieValue, "=");
				if (cookieKV != null && cookieKV.length == 2 && StringUtils.equals(cookieKV[0], key)) {
					return cookieKV[1];
				}
			}
		}
		return StringUtils.EMPTY;
	}
}
