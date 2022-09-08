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

package com.tencent.cloud.polaris.config.spring.property;

import java.util.Set;
import java.util.Stack;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.util.StringUtils;

/**
 * Placeholder helper functions.
 * <p>
 * This source file was originally from:
 * <code><a href=https://github.com/apolloconfig/apollo/blob/master/apollo-client/src/main/java/com/ctrip/framework/apollo/spring/property/PlaceholderHelper.java>
 *     PlaceholderHelper</a></code>
 *
 * @author weihubeats 2022-7-10
 */
public class PlaceholderHelper {

	private static final String PLACEHOLDER_PREFIX = "${";
	private static final String PLACEHOLDER_SUFFIX = "}";
	private static final String VALUE_SEPARATOR = ":";
	private static final String SIMPLE_PLACEHOLDER_PREFIX = "{";
	private static final String EXPRESSION_PREFIX = "#{";
	private static final String EXPRESSION_SUFFIX = "}";

	/**
	 * Resolve placeholder property values, e.g.
	 * @param beanFactory beanFactory
	 * @param beanName beanName
	 * @param placeholder placeholder
	 * @return "${somePropertyValue}" -> "the actual property value"
	 */
	public Object resolvePropertyValue(ConfigurableBeanFactory beanFactory, String beanName, String placeholder) {
		// resolve string value
		String strVal = beanFactory.resolveEmbeddedValue(placeholder);

		BeanDefinition bd = (beanFactory.containsBean(beanName) ? beanFactory
				.getMergedBeanDefinition(beanName) : null);

		// resolve expressions like "#{systemProperties.myProp}"
		return evaluateBeanDefinitionString(beanFactory, strVal, bd);
	}

	private Object evaluateBeanDefinitionString(ConfigurableBeanFactory beanFactory, String value,
			BeanDefinition beanDefinition) {
		if (beanFactory.getBeanExpressionResolver() == null) {
			return value;
		}
		Scope scope = (beanDefinition != null && beanDefinition.getScope() != null ? beanFactory
				.getRegisteredScope(beanDefinition.getScope()) : null);
		return beanFactory.getBeanExpressionResolver()
				.evaluate(value, new BeanExpressionContext(beanFactory, scope));
	}

	/**
	 *
	 * @param propertyString propertyString
	 * @return
	 * Extract keys from placeholder, e.g.
	 * <li>${some.key} => "some.key"</li>
	 * <li>${some.key:${some.other.key:100}} => "some.key", "some.other.key"</li>
	 * <li>${${some.key}} => "some.key"</li>
	 * <li>${${some.key:other.key}} => "some.key"</li>
	 * <li>${${some.key}:${another.key}} => "some.key", "another.key"</li>
	 * <li>#{new java.text.SimpleDateFormat('${some.key}').parse('${another.key}')} => "some.key", "another.key"</li>
	 */
	public Set<String> extractPlaceholderKeys(String propertyString) {
		Set<String> placeholderKeys = Sets.newHashSet();

		if (!isPlaceholder(propertyString)) {
			return placeholderKeys;
		}

		Stack<String> stack = new Stack<>();
		stack.push(propertyString);

		while (!stack.isEmpty()) {
			String strVal = stack.pop();
			int startIndex = strVal.indexOf(PLACEHOLDER_PREFIX);
			if (startIndex == -1) {
				placeholderKeys.add(strVal);
				continue;
			}
			int endIndex = findPlaceholderEndIndex(strVal, startIndex);
			if (endIndex == -1) {
				// invalid placeholder?
				continue;
			}

			String placeholderCandidate = strVal.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);

			// ${some.key:other.key}
			if (placeholderCandidate.startsWith(PLACEHOLDER_PREFIX)) {
				stack.push(placeholderCandidate);
			}
			else {
				// some.key:${some.other.key:100}
				int separatorIndex = placeholderCandidate.indexOf(VALUE_SEPARATOR);

				if (separatorIndex == -1) {
					stack.push(placeholderCandidate);
				}
				else {
					stack.push(placeholderCandidate.substring(0, separatorIndex));
					String defaultValuePart =
							normalizeToPlaceholder(placeholderCandidate.substring(separatorIndex + VALUE_SEPARATOR.length()));
					if (!Strings.isNullOrEmpty(defaultValuePart)) {
						stack.push(defaultValuePart);
					}
				}
			}

			// has remaining part, e.g. ${a}.${b}
			if (endIndex + PLACEHOLDER_SUFFIX.length() < strVal.length() - 1) {
				String remainingPart = normalizeToPlaceholder(strVal.substring(endIndex + PLACEHOLDER_SUFFIX.length()));
				if (!Strings.isNullOrEmpty(remainingPart)) {
					stack.push(remainingPart);
				}
			}
		}

		return placeholderKeys;
	}

	private boolean isPlaceholder(String propertyString) {
		return !Strings.isNullOrEmpty(propertyString) &&
				(isNormalizedPlaceholder(propertyString) || isExpressionWithPlaceholder(propertyString));
	}

	private boolean isNormalizedPlaceholder(String propertyString) {
		return propertyString.startsWith(PLACEHOLDER_PREFIX) && propertyString.contains(PLACEHOLDER_SUFFIX);
	}

	private boolean isExpressionWithPlaceholder(String propertyString) {
		return propertyString.startsWith(EXPRESSION_PREFIX) && propertyString.contains(EXPRESSION_SUFFIX)
				&& propertyString.contains(PLACEHOLDER_PREFIX) && propertyString.contains(PLACEHOLDER_SUFFIX);
	}

	private String normalizeToPlaceholder(String strVal) {
		int startIndex = strVal.indexOf(PLACEHOLDER_PREFIX);
		if (startIndex == -1) {
			return null;
		}
		int endIndex = strVal.lastIndexOf(PLACEHOLDER_SUFFIX);
		if (endIndex == -1) {
			return null;
		}

		return strVal.substring(startIndex, endIndex + PLACEHOLDER_SUFFIX.length());
	}

	private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
		int index = startIndex + PLACEHOLDER_PREFIX.length();
		int withinNestedPlaceholder = 0;
		while (index < buf.length()) {
			if (StringUtils.substringMatch(buf, index, PLACEHOLDER_SUFFIX)) {
				if (withinNestedPlaceholder > 0) {
					withinNestedPlaceholder--;
					index = index + PLACEHOLDER_SUFFIX.length();
				}
				else {
					return index;
				}
			}
			else if (StringUtils.substringMatch(buf, index, SIMPLE_PLACEHOLDER_PREFIX)) {
				withinNestedPlaceholder++;
				index = index + SIMPLE_PLACEHOLDER_PREFIX.length();
			}
			else {
				index++;
			}
		}
		return -1;
	}
}
