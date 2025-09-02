/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.tsf.core.entity.Tag;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ReflectionUtils}.
 *
 * @author Haotian Zhang
 */
public class ReflectionUtilsTest {

	@Test
	public void testWritableBeanField() {
		Field field = ReflectionUtils.findField(Tag.class, "key");
		assertThat(field).isNotNull();
		assertThat(ReflectionUtils.writableBeanField(field)).isTrue();
	}

	@Test
	public void testCapitalize() {
		String name = "test";
		assertThat(ReflectionUtils.capitalize(name)).isEqualTo("Test");

		String gender = "";
		assertThat(ReflectionUtils.capitalize(gender)).isEqualTo("");
	}

	@Test
	public void testGetFieldValue() {
		Tag tag = new Tag("key", "value");
		assertThat(ReflectionUtils.getFieldValue(tag, "key")).isEqualTo("key");
		assertThat(ReflectionUtils.getFieldValue(tag, "value")).isEqualTo("value");
		assertThat(ReflectionUtils.getFieldValue(tag, "flags")).isNotNull().isInstanceOf(Set.class);
	}

	@Test
	public void testSetFieldValue() {
		Tag tag = new Tag("key", null);
		ReflectionUtils.setFieldValue(tag, "key", "newKey");
		assertThat(ReflectionUtils.getFieldValue(tag, "key")).isEqualTo("newKey");
		ReflectionUtils.setFieldValue(tag, "value1", "newValue");
	}
}
