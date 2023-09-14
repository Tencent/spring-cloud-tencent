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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

/**
 * Parse labels from ServerWebExchange and HttpRequest.
 * @author lepdou 2022-07-11
 */
public final class SpringWebExpressionLabelUtils {

	private SpringWebExpressionLabelUtils() {
	}

	public static Map<String, String> resolve(ServerWebExchange exchange, Set<String> labelKeys) {
		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		Map<String, String> labels = new HashMap<>();

		for (String labelKey : labelKeys) {
			if (!ExpressionLabelUtils.isExpressionLabel(labelKey)) {
				continue;
			}
			if (ExpressionLabelUtils.isHeaderLabel(labelKey)) {
				String headerKey = ExpressionLabelUtils.parseHeaderKey(labelKey);
				if (StringUtils.isBlank(headerKey)) {
					continue;
				}
				labels.put(labelKey, getHeaderValue(exchange.getRequest(), headerKey));
			}
			else if (ExpressionLabelUtils.isQueryLabel(labelKey)) {
				String queryKey = ExpressionLabelUtils.parseQueryKey(labelKey);
				if (StringUtils.isBlank(queryKey)) {
					continue;
				}
				labels.put(labelKey, getQueryValue(exchange.getRequest(), queryKey));
			}
			else if (ExpressionLabelUtils.isCookieLabel(labelKey)) {
				String cookieKey = ExpressionLabelUtils.parseCookieKey(labelKey);
				if (StringUtils.isBlank(cookieKey)) {
					continue;
				}
				labels.put(labelKey, getCookieValue(exchange.getRequest(), cookieKey));
			}
			else if (ExpressionLabelUtils.isMethodLabel(labelKey)) {
				labels.put(labelKey, exchange.getRequest().getMethodValue());
			}
			else if (ExpressionLabelUtils.isUriLabel(labelKey)) {
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
			if (!ExpressionLabelUtils.isExpressionLabel(labelKey)) {
				continue;
			}
			if (ExpressionLabelUtils.isHeaderLabel(labelKey)) {
				String headerKey = ExpressionLabelUtils.parseHeaderKey(labelKey);
				if (StringUtils.isBlank(headerKey)) {
					continue;
				}
				labels.put(labelKey, getHeaderValue(request, headerKey));
			}
			else if (ExpressionLabelUtils.isQueryLabel(labelKey)) {
				String queryKey = ExpressionLabelUtils.parseQueryKey(labelKey);
				if (StringUtils.isBlank(queryKey)) {
					continue;
				}
				labels.put(labelKey, getQueryValue(request, queryKey));
			}
			else if (ExpressionLabelUtils.isCookieLabel(labelKey)) {
				String cookieKey = ExpressionLabelUtils.parseCookieKey(labelKey);
				if (StringUtils.isBlank(cookieKey)) {
					continue;
				}
				labels.put(labelKey, getCookieValue(request, cookieKey));
			}
			else if (ExpressionLabelUtils.isMethodLabel(labelKey)) {
				labels.put(labelKey, request.getMethodValue());
			}
			else if (ExpressionLabelUtils.isUriLabel(labelKey)) {
				labels.put(labelKey, request.getURI().getPath());
			}
		}

		return labels;
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
		return ExpressionLabelUtils.getQueryValue(query, key);
	}

	public static String getCookieValue(HttpRequest request, String key) {
		String first = request.getHeaders().getFirst(HttpHeaders.COOKIE);
		if (StringUtils.isEmpty(first)) {
			return StringUtils.EMPTY;
		}
		String[] cookieArray = StringUtils.split(first, ";");
		for (String cookieItem : cookieArray) {
			String[] cookieKv = StringUtils.split(cookieItem, "=");
			if (cookieKv != null && cookieKv.length == 2 && StringUtils.equals(cookieKv[0], key)) {
				return cookieKv[1];
			}
		}
		return StringUtils.EMPTY;
	}
}
