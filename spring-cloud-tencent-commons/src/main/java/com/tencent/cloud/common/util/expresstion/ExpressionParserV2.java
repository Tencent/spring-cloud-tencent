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
 * New custom expression resolver like $query.key、$header.key.
 * Old expression like ${http.query.key}、${http.header.key}
 * @author lepdou 2022-10-08
 */
public class ExpressionParserV2 implements ExpressionParser {

	private static final String LABEL_HEADER_PREFIX = "$header.";
	private static final int LABEL_HEADER_PREFIX_LEN = LABEL_HEADER_PREFIX.length();
	private static final String LABEL_QUERY_PREFIX = "$query.";
	private static final int LABEL_QUERY_PREFIX_LEN = LABEL_QUERY_PREFIX.length();
	private static final String LABEL_METHOD = "$method";
	private static final String LABEL_PATH = "$path";
	private static final String LABEL_CALLER_IP = "$caller_ip";
	private static final String LABEL_PREFIX = "$";


	@Override
	public boolean isExpressionLabel(String expression) {
		return StringUtils.startsWith(expression, LABEL_PREFIX);
	}

	@Override
	public boolean isHeaderLabel(String expression) {
		return StringUtils.startsWith(expression, LABEL_HEADER_PREFIX);
	}

	@Override
	public String parseHeaderKey(String expression) {
		return StringUtils.substring(expression, LABEL_HEADER_PREFIX_LEN);
	}

	@Override
	public boolean isQueryLabel(String expression) {
		return StringUtils.startsWith(expression, LABEL_QUERY_PREFIX);
	}

	@Override
	public String parseQueryKey(String expression) {
		return StringUtils.substring(expression, LABEL_QUERY_PREFIX_LEN);
	}

	@Override
	public boolean isCookieLabel(String expression) {
		return false;
	}

	@Override
	public String parseCookieKey(String expression) {
		return null;
	}

	@Override
	public boolean isMethodLabel(String expression) {
		return StringUtils.equalsIgnoreCase(expression, LABEL_METHOD);
	}

	@Override
	public boolean isUriLabel(String expression) {
		return StringUtils.equalsIgnoreCase(expression, LABEL_PATH);
	}

	@Override
	public boolean isCallerIPLabel(String expression) {
		return StringUtils.equalsIgnoreCase(expression, LABEL_CALLER_IP);
	}
}
