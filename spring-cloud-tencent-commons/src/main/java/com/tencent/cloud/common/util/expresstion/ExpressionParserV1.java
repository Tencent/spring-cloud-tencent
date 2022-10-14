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

package com.tencent.cloud.common.util.expresstion;

import org.apache.commons.lang.StringUtils;

/**
 * Old custom expression resolver like ${http.query.key}、${http.header.key}.
 * New expression like $query.key、$header.key
 * @author lepdou 2022-10-08
 */
public class ExpressionParserV1 implements ExpressionParser {

	private static final String LABEL_HEADER_PREFIX = "${http.header.";
	private static final int LABEL_HEADER_PREFIX_LEN = LABEL_HEADER_PREFIX.length();
	private static final String LABEL_QUERY_PREFIX = "${http.query.";
	private static final int LABEL_QUERY_PREFIX_LEN = LABEL_QUERY_PREFIX.length();
	private static final String LABEL_COOKIE_PREFIX = "${http.cookie.";
	private static final int LABEL_COOKIE_PREFIX_LEN = LABEL_COOKIE_PREFIX.length();
	private static final String LABEL_METHOD = "${http.method}";
	private static final String LABEL_URI = "${http.uri}";
	private static final String LABEL_CALLER_IP = "${http.caller.ip}";
	private static final String LABEL_PREFIX = "${";
	private static final String LABEL_SUFFIX = "}";

	@Override
	public boolean isExpressionLabel(String labelKey) {
		if (StringUtils.isEmpty(labelKey)) {
			return false;
		}
		return StringUtils.startsWith(labelKey, LABEL_PREFIX) && StringUtils.endsWith(labelKey, LABEL_SUFFIX);
	}

	@Override
	public boolean isHeaderLabel(String expression) {
		return StringUtils.startsWith(expression, LABEL_HEADER_PREFIX) && StringUtils.endsWith(expression, LABEL_SUFFIX);
	}

	@Override
	public String parseHeaderKey(String expression) {
		return StringUtils.substring(expression, LABEL_HEADER_PREFIX_LEN, expression.length() - 1);
	}

	@Override
	public boolean isQueryLabel(String expression) {
		return StringUtils.startsWith(expression, LABEL_QUERY_PREFIX) && StringUtils.endsWith(expression, LABEL_SUFFIX);
	}

	@Override
	public String parseQueryKey(String expression) {
		return StringUtils.substring(expression, LABEL_QUERY_PREFIX_LEN, expression.length() - 1);
	}

	@Override
	public boolean isCookieLabel(String expression) {
		return StringUtils.startsWith(expression, LABEL_COOKIE_PREFIX) && StringUtils.endsWith(expression, LABEL_SUFFIX);
	}

	@Override
	public String parseCookieKey(String expression) {
		return StringUtils.substring(expression, LABEL_COOKIE_PREFIX_LEN, expression.length() - 1);
	}

	@Override
	public boolean isMethodLabel(String expression) {
		return StringUtils.equalsIgnoreCase(expression, LABEL_METHOD);
	}

	@Override
	public boolean isUriLabel(String expression) {
		return StringUtils.equalsIgnoreCase(expression, LABEL_URI);
	}

	@Override
	public boolean isCallerIPLabel(String expression) {
		return StringUtils.equalsIgnoreCase(expression, LABEL_CALLER_IP);
	}
}
