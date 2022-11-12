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

package com.tencent.cloud.common.util;

import java.lang.reflect.Field;

import org.springframework.util.ClassUtils;

import static java.util.Locale.ENGLISH;

/**
 * Reflection Utils.
 *
 * @author Haotian Zhang
 */
public final class ReflectionUtils extends org.springframework.util.ReflectionUtils {

	private final static String SET_PREFIX = "set";
	private ReflectionUtils() {
	}

	public static boolean writableBeanField(Field field) {
		String fieldName = field.getName();
		String setMethodName = SET_PREFIX + capitalize(fieldName);
		return ClassUtils.hasMethod(field.getDeclaringClass(), setMethodName, field.getType());
	}

	public static String capitalize(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		return name.substring(0, 1).toUpperCase(ENGLISH) + name.substring(1);
	}

	public static Object getFieldValue(Object instance, String fieldName) {
		Field field = org.springframework.util.ReflectionUtils.findField(instance.getClass(), fieldName);
		if (field == null) {
			return null;
		}

		field.setAccessible(true);
		try {
			return field.get(instance);
		}
		catch (IllegalAccessException e) {
			// ignore
		}
		finally {
			field.setAccessible(false);
		}
		return null;
	}
}
