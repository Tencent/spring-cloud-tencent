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

package com.tencent.cloud.common.rule;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import org.springframework.util.CollectionUtils;

/**
 * The condition operation.
 * @author lepdou 2022-07-11
 */
public enum Operation {

	/**
	 * case sensitive string equals.
	 */
	EQUALS("EQUALS"),
	/**
	 * case sensitive string not equals.
	 */
	NOT_EQUALS("NOT_EQUALS"),
	/**
	 * whether element in collection.
	 */
	IN("IN"),
	/**
	 * whether element not in collection.
	 */
	NOT_IN("NOT_IN"),
	/**
	 * regex operation.
	 */
	REGEX("REGEX"),
	/**
	 * whether element is blank.
	 */
	BLANK("BLANK"),
	/**
	 * whether element is not blank.
	 */
	NOT_BLANK("NOT_BLANK");

	private final String value;

	Operation(String value) {
		this.value = value;
	}

	public static boolean match(List<String> expectedValues, String actualValue, String rawOperation) {
		String firstExpectedValue = null;
		if (!CollectionUtils.isEmpty(expectedValues)) {
			firstExpectedValue = expectedValues.get(0);
		}

		switch (getOperation(rawOperation)) {
		case EQUALS:
			return firstExpectedValue != null && StringUtils.equals(actualValue, firstExpectedValue);
		case NOT_EQUALS:
			return firstExpectedValue == null || !StringUtils.equals(actualValue, firstExpectedValue);
		case BLANK:
			return StringUtils.isBlank(actualValue);
		case NOT_BLANK:
			return !StringUtils.isBlank(actualValue);
		case IN:
			if (CollectionUtils.isEmpty(expectedValues)) {
				return false;
			}
			return expectedValues.contains(actualValue);
		case NOT_IN:
			if (CollectionUtils.isEmpty(expectedValues)) {
				return true;
			}
			return !expectedValues.contains(actualValue);
		case REGEX:
			if (firstExpectedValue == null) {
				return false;
			}
			Pattern r = Pattern.compile(firstExpectedValue);
			return r.matcher(actualValue).matches();
		default:
			return false;
		}
	}

	public static Operation getOperation(String operation) {
		if (StringUtils.equalsIgnoreCase(operation, EQUALS.value)) {
			return EQUALS;
		}
		if (StringUtils.equalsIgnoreCase(operation, NOT_EQUALS.value)) {
			return NOT_EQUALS;
		}
		if (StringUtils.equalsIgnoreCase(operation, IN.value)) {
			return IN;
		}
		if (StringUtils.equalsIgnoreCase(operation, NOT_IN.value)) {
			return NOT_IN;
		}
		if (StringUtils.equalsIgnoreCase(operation, REGEX.value)) {
			return REGEX;
		}
		if (StringUtils.equalsIgnoreCase(operation, BLANK.value)) {
			return BLANK;
		}
		if (StringUtils.equalsIgnoreCase(operation, NOT_BLANK.value)) {
			return NOT_BLANK;
		}
		throw new RuntimeException("Unsupported operation. operation = " + operation);
	}

	public String getValue() {
		return value;
	}
}
