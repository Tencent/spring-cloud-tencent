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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test for {@link Operation}.
 * @author lepdou 2022-07-12
 */
@RunWith(MockitoJUnitRunner.class)
public class OperationTest {

	@Test
	public void testEqual() {
		Assert.assertTrue(Operation.match(Collections.singletonList("v1"), "v1", Operation.EQUALS.getValue()));
		Assert.assertFalse(Operation.match(Collections.singletonList("v1"), "v2", Operation.EQUALS.getValue()));
		Assert.assertFalse(Operation.match(Collections.singletonList(""), "v2", Operation.EQUALS.getValue()));
		Assert.assertFalse(Operation.match(Collections.singletonList("v1"), "", Operation.EQUALS.getValue()));
		Assert.assertFalse(Operation.match(Collections.singletonList("v1"), null, Operation.EQUALS.getValue()));
		Assert.assertFalse(Operation.match(Collections.emptyList(), "v1", Operation.EQUALS.getValue()));
	}

	@Test
	public void testNotEqual() {
		Assert.assertFalse(Operation.match(Collections.singletonList("v1"), "v1", Operation.NOT_EQUALS.getValue()));
		Assert.assertTrue(Operation.match(Collections.singletonList("v1"), "v2", Operation.NOT_EQUALS.getValue()));
		Assert.assertTrue(Operation.match(Collections.singletonList(""), "v2", Operation.NOT_EQUALS.getValue()));
		Assert.assertTrue(Operation.match(Collections.singletonList("v1"), "", Operation.NOT_EQUALS.getValue()));
		Assert.assertTrue(Operation.match(Collections.singletonList("v1"), null, Operation.NOT_EQUALS.getValue()));
		Assert.assertTrue(Operation.match(Collections.emptyList(), "v1", Operation.NOT_EQUALS.getValue()));
	}

	@Test
	public void testIn() {
		Assert.assertTrue(Operation.match(Arrays.asList("v1", "v2", "v3"), "v1", Operation.IN.getValue()));
		Assert.assertTrue(Operation.match(Arrays.asList("v1", "v2", "v3"), "v2", Operation.IN.getValue()));
		Assert.assertFalse(Operation.match(Arrays.asList("v1", "v2", "v3"), "v4", Operation.IN.getValue()));
		Assert.assertFalse(Operation.match(Arrays.asList("v1", "v2", "v3"), "", Operation.IN.getValue()));
		Assert.assertFalse(Operation.match(Arrays.asList("v1", "v2", "v3"), null, Operation.IN.getValue()));
		Assert.assertFalse(Operation.match(Collections.emptyList(), null, Operation.IN.getValue()));
	}

	@Test
	public void testNotIn() {
		Assert.assertFalse(Operation.match(Arrays.asList("v1", "v2", "v3"), "v1", Operation.NOT_IN.getValue()));
		Assert.assertFalse(Operation.match(Arrays.asList("v1", "v2", "v3"), "v2", Operation.NOT_IN.getValue()));
		Assert.assertTrue(Operation.match(Arrays.asList("v1", "v2", "v3"), "v4", Operation.NOT_IN.getValue()));
		Assert.assertTrue(Operation.match(Arrays.asList("v1", "v2", "v3"), "", Operation.NOT_IN.getValue()));
		Assert.assertTrue(Operation.match(Arrays.asList("v1", "v2", "v3"), null, Operation.NOT_IN.getValue()));
		Assert.assertTrue(Operation.match(Collections.emptyList(), null, Operation.NOT_IN.getValue()));
	}

	@Test
	public void testEmpty() {
		Assert.assertTrue(Operation.match(Collections.singletonList("v1"), null, Operation.BLANK.getValue()));
		Assert.assertTrue(Operation.match(Collections.singletonList("v1"), "", Operation.BLANK.getValue()));
		Assert.assertTrue(Operation.match(Collections.emptyList(), null, Operation.BLANK.getValue()));
	}

	@Test
	public void testNotEmpty() {
		Assert.assertFalse(Operation.match(Collections.singletonList("v1"), null, Operation.NOT_BLANK.getValue()));
		Assert.assertFalse(Operation.match(Collections.singletonList("v1"), "", Operation.NOT_BLANK.getValue()));
		Assert.assertFalse(Operation.match(Collections.emptyList(), null, Operation.NOT_BLANK.getValue()));
		Assert.assertTrue(Operation.match(Collections.emptyList(), "v1", Operation.NOT_BLANK.getValue()));
	}

	@Test
	public void testRegex() {
		Assert.assertTrue(Operation.match(Collections.singletonList("v[1~10]"), "v1", Operation.REGEX.getValue()));
		Assert.assertFalse(Operation.match(Collections.singletonList("v[1~10]"), "v12", Operation.REGEX.getValue()));
		Assert.assertFalse(Operation.match(Collections.singletonList("v[1~10]*"), "v12", Operation.REGEX.getValue()));
	}
}
