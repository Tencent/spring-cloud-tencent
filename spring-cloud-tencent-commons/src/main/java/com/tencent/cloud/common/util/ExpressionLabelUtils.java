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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

/**
 * the utils for parse label expression.
 *
 *@author lepdou 2022-05-13
 */
public class ExpressionLabelUtils {

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
	 * the prefix of expression.
	 */
	public static final String LABEL_PREFIX = "${";
	/**
	 * the suffix of expression.
	 */
	public static final String LABEL_SUFFIX = "}";
	/**
	 * the escape prefix of label.
	 */
	public static final String LABEL_ESCAPE_PREFIX = "##@$@##";

	public static boolean isExpressionLabel(String labelKey) {
		if (StringUtils.isEmpty(labelKey)) {
			return false;
		}
		if (StringUtils.equalsIgnoreCase(LABEL_METHOD, labelKey) ||
				StringUtils.startsWithIgnoreCase(LABEL_URI, labelKey)) {
			return true;
		}
		return (StringUtils.startsWithIgnoreCase(labelKey, LABEL_HEADER_PREFIX) ||
				StringUtils.startsWithIgnoreCase(labelKey, LABEL_QUERY_PREFIX) ||
				StringUtils.startsWithIgnoreCase(labelKey, LABEL_COOKIE_PREFIX))
				&& StringUtils.endsWith(labelKey, LABEL_SUFFIX);
	}

	public static String escape(String str) {
		return StringUtils.replace(str, LABEL_PREFIX, LABEL_ESCAPE_PREFIX);
	}

	public static String unescape(String str) {
		return StringUtils.replace(str, LABEL_ESCAPE_PREFIX, LABEL_PREFIX);
	}

	public static Map<String, String> resolve(HttpServletRequest request, Set<String> labelKeys) {
		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		Map<String, String> labels = new HashMap<>();

		for (String labelKey : labelKeys) {
			if (!isExpressionLabel(labelKey)) {
				continue;
			}
			if (StringUtils.startsWithIgnoreCase(labelKey, LABEL_HEADER_PREFIX)) {
				String headerKey = parseHeaderKey(labelKey);
				if (StringUtils.isBlank(headerKey)) {
					continue;
				}
				labels.put(labelKey, request.getHeader(headerKey));
			}
			else if (StringUtils.startsWithIgnoreCase(labelKey, LABEL_QUERY_PREFIX)) {
				String queryKey = parseQueryKey(labelKey);
				if (StringUtils.isBlank(queryKey)) {
					continue;
				}
				labels.put(labelKey, getQueryValue(request.getQueryString(), queryKey));
			}
			else if (StringUtils.startsWithIgnoreCase(labelKey, LABEL_COOKIE_PREFIX)) {
				String cookieKey = parseCookieKey(labelKey);
				if (StringUtils.isBlank(cookieKey)) {
					continue;
				}
				labels.put(labelKey, getCookieValue(request.getCookies(), cookieKey));
			}
			else if (StringUtils.equalsIgnoreCase(LABEL_METHOD, labelKey)) {
				labels.put(labelKey, request.getMethod());
			}
			else if (StringUtils.equalsIgnoreCase(LABEL_URI, labelKey)) {
				labels.put(labelKey, request.getRequestURI());
			}
		}

		return labels;
	}

	public static Map<String, String> resolve(ServerWebExchange exchange, Set<String> labelKeys) {
		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		Map<String, String> labels = new HashMap<>();

		for (String labelKey : labelKeys) {
			if (!isExpressionLabel(labelKey)) {
				continue;
			}
			if (StringUtils.startsWithIgnoreCase(labelKey, LABEL_HEADER_PREFIX)) {
				String headerKey = parseHeaderKey(labelKey);
				if (StringUtils.isBlank(headerKey)) {
					continue;
				}
				labels.put(labelKey, getHeaderValue(exchange.getRequest(), headerKey));
			}
			else if (StringUtils.startsWithIgnoreCase(labelKey, LABEL_QUERY_PREFIX)) {
				String queryKey = parseQueryKey(labelKey);
				if (StringUtils.isBlank(queryKey)) {
					continue;
				}
				labels.put(labelKey, getQueryValue(exchange.getRequest(), queryKey));
			}
			else if (StringUtils.startsWithIgnoreCase(labelKey, LABEL_COOKIE_PREFIX)) {
				String cookieKey = parseCookieKey(labelKey);
				if (StringUtils.isBlank(cookieKey)) {
					continue;
				}
				labels.put(labelKey, getCookieValue(exchange.getRequest(), cookieKey));
			}
			else if (StringUtils.equalsIgnoreCase(LABEL_METHOD, labelKey)) {
				labels.put(labelKey, exchange.getRequest().getMethodValue());
			}
			else if (StringUtils.equalsIgnoreCase(LABEL_URI, labelKey)) {
				labels.put(labelKey, exchange.getRequest().getURI().getPath());
			}
		}

		return labels;
	}

	public static Map<String, String> resolve(HttpRequest request, Set<String> labelKeys) {
		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		Map<String, String> labels = new HashMap<>();

		for (String labelKey : labelKeys) {
			if (!isExpressionLabel(labelKey)) {
				continue;
			}
			if (StringUtils.startsWithIgnoreCase(labelKey, LABEL_HEADER_PREFIX)) {
				String headerKey = parseHeaderKey(labelKey);
				if (StringUtils.isBlank(headerKey)) {
					continue;
				}
				labels.put(labelKey, getHeaderValue(request, headerKey));
			}
			else if (StringUtils.startsWithIgnoreCase(labelKey, LABEL_QUERY_PREFIX)) {
				String queryKey = parseQueryKey(labelKey);
				if (StringUtils.isBlank(queryKey)) {
					continue;
				}
				labels.put(labelKey, getQueryValue(request, queryKey));
			}
			else if (StringUtils.equalsIgnoreCase(LABEL_METHOD, labelKey)) {
				labels.put(labelKey, request.getMethodValue());
			}
			else if (StringUtils.equalsIgnoreCase(LABEL_URI, labelKey)) {
				labels.put(labelKey, request.getURI().getPath());
			}
		}

		return labels;
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

	public static String getCookieValue(Cookie[] cookies, String key) {
		if (cookies == null || cookies.length == 0) {
			return StringUtils.EMPTY;
		}
		for (Cookie cookie : cookies) {
			if (StringUtils.equals(cookie.getName(), key)) {
				return cookie.getValue();
			}
		}
		return StringUtils.EMPTY;
	}

	public static String getHeaderValue(ServerHttpRequest request, String key) {
		String value = request.getHeaders().getFirst(key);
		if (value == null) {
			return StringUtils.EMPTY;
		}
		return value;
	}

	public static String getQueryValue(ServerHttpRequest request, String key) {
		MultiValueMap<String, String> queries = request.getQueryParams();
		if (CollectionUtils.isEmpty(queries)) {
			return StringUtils.EMPTY;
		}
		String value = queries.getFirst(key);
		if (value == null) {
			return StringUtils.EMPTY;
		}
		return value;
	}

	public static String getCookieValue(ServerHttpRequest request, String key) {
		HttpCookie cookie = request.getCookies().getFirst(key);
		if (cookie == null) {
			return StringUtils.EMPTY;
		}
		return cookie.getValue();
	}

	public static String getHeaderValue(HttpRequest request, String key) {
		HttpHeaders headers = request.getHeaders();
		return headers.getFirst(key);
	}

	public static String getQueryValue(HttpRequest request, String key) {
		String query = request.getURI().getQuery();
		return getQueryValue(query, key);
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
