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

/**
 * Expression parser for rate limit rule and router rule.
 * @author lepdou 2022-10-08
 */
public interface ExpressionParser {

	/**
	 * whether is valid expression.
	 * @param expression the expression
	 * @return true if is valid
	 */
	boolean isExpressionLabel(String expression);

	/**
	 * whether is header expression.
	 * @param expression the expression
	 * @return true if is header expression
	 */
	boolean isHeaderLabel(String expression);

	/**
	 * parse label from header expression.
	 * @param expression the expression
	 * @return parsed key from expression
	 */
	String parseHeaderKey(String expression);

	/**
	 * whether is query expression.
	 * @param expression the expression
	 * @return true if is query expression
	 */
	boolean isQueryLabel(String expression);

	/**
	 * parse label from query expression.
	 * @param expression the expression
	 * @return parsed key from expression
	 */
	String parseQueryKey(String expression);

	/**
	 * whether is cookie expression.
	 * @param expression the expression
	 * @return true if is cookie expression
	 */
	boolean isCookieLabel(String expression);

	/**
	 * parse label from cookie expression.
	 * @param expression the expression
	 * @return parsed cookie key from expression
	 */
	String parseCookieKey(String expression);

	/**
	 * whether is method expression.
	 * @param expression the expression
	 * @return true if is method expression
	 */
	boolean isMethodLabel(String expression);

	/**
	 * whether is uri/path expression.
	 * @param expression the expression
	 * @return true if is uri/path expression
	 */
	boolean isUriLabel(String expression);

	/**
	 * whether is caller ip expression.
	 * @param expression the expression
	 * @return true if is caller ip expression
	 */
	boolean isCallerIPLabel(String expression);
}
