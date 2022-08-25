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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import org.springframework.util.CollectionUtils;

/**
 * Parse labels from HttpServletRequest.
 * @author lepdou 2022-07-11
 */
public final class ServletExpressionLabelUtils {

	private ServletExpressionLabelUtils() {
	}

	public static Map<String, String> resolve(HttpServletRequest request, Set<String> labelKeys) {
		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		Map<String, String> labels = new HashMap<>();

		for (String labelKey : labelKeys) {
			if (!ExpressionLabelUtils.isExpressionLabel(labelKey)) {
				continue;
			}
			if (StringUtils.startsWithIgnoreCase(labelKey, ExpressionLabelUtils.LABEL_HEADER_PREFIX)) {
				String headerKey = ExpressionLabelUtils.parseHeaderKey(labelKey);
				if (StringUtils.isBlank(headerKey)) {
					continue;
				}
				labels.put(labelKey, request.getHeader(headerKey));
			}
			else if (StringUtils.startsWithIgnoreCase(labelKey, ExpressionLabelUtils.LABEL_QUERY_PREFIX)) {
				String queryKey = ExpressionLabelUtils.parseQueryKey(labelKey);
				if (StringUtils.isBlank(queryKey)) {
					continue;
				}
				labels.put(labelKey, ExpressionLabelUtils.getQueryValue(request.getQueryString(), queryKey));
			}
			else if (StringUtils.startsWithIgnoreCase(labelKey, ExpressionLabelUtils.LABEL_COOKIE_PREFIX)) {
				String cookieKey = ExpressionLabelUtils.parseCookieKey(labelKey);
				if (StringUtils.isBlank(cookieKey)) {
					continue;
				}
				labels.put(labelKey, getCookieValue(request.getCookies(), cookieKey));
			}
			else if (StringUtils.equalsIgnoreCase(ExpressionLabelUtils.LABEL_METHOD, labelKey)) {
				labels.put(labelKey, request.getMethod());
			}
			else if (StringUtils.equalsIgnoreCase(ExpressionLabelUtils.LABEL_URI, labelKey)) {
				labels.put(labelKey, request.getRequestURI());
			}
		}

		return labels;
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
}
