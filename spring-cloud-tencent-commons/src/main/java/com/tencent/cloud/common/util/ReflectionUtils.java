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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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

	/**
	 * get property of class object by property name.
	 *
	 * @param target    object
	 * @param fieldName property name of class object
	 * @return value
	 */
	public static Object getObjectByFieldName(Object target, String fieldName) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(target);
		}
		catch (Exception e) {
			throw new RuntimeException("getObjectByFieldName", e);
		}
	}

	/**
	 * get property of parent class object by property name.
	 *
	 * @param target    object
	 * @param fieldName property name of parent class object
	 * @return value
	 */
	public static Object getSuperObjectByFieldName(Object target, String fieldName) {
		try {
			Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(target);
		}
		catch (Exception e) {
			throw new RuntimeException("getSuperObjectByFieldName", e);
		}
	}


	/**
	 * set property of class object by property name.
	 *
	 * @param target    object
	 * @param fieldName property name of class object
	 * @param value     new value
	 */
	public static void setValueByFieldName(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			setValue(target, field, value);
		}
		catch (Exception e) {
			throw new RuntimeException("setValueByFieldName", e);
		}
	}

	/**
	 * set property of parent class object by property name.
	 *
	 * @param target    object
	 * @param fieldName property name of parent class object
	 * @param value     new value
	 */
	public static void setSuperValueByFieldName(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
			setValue(target, field, value);
		}
		catch (Exception e) {
			throw new RuntimeException("setSuperValueByFieldName", e);
		}
	}

	private static void setValue(Object target, Field field, Object value) {
		try {
			Field modifiers = getModifiersField();
			modifiers.setAccessible(true);
			modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			field.setAccessible(true);
			field.set(target, value);
		}
		catch (Exception e) {
			throw new RuntimeException("setValue", e);
		}
	}

	private static Field getModifiersField() throws Exception {
		Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
		getDeclaredFields0.setAccessible(true);
		Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
		Field modifierField = null;
		for (Field f : fields) {
			if ("modifiers".equals(f.getName())) {
				modifierField = f;
				break;
			}
		}
		return modifierField;
	}
}
