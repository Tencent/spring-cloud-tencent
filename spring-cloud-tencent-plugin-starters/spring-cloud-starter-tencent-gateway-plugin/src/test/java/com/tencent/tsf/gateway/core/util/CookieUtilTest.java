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

package com.tencent.tsf.gateway.core.util;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link CookieUtil}.
 *
 * @author Haotian Zhang
 */
public class CookieUtilTest {

	@Test
	public void testBuildCookie() {
		StringBuilder stringBuilder1 = new StringBuilder();
		CookieUtil.buildCookie(stringBuilder1, new HashMap<String, String>() {{
			put("test1", "123");
			put("test2", "123");
		}});

		StringBuilder stringBuilder2 = new StringBuilder();
		stringBuilder2.append("aaa=bbb");
		CookieUtil.buildCookie(stringBuilder2, new HashMap<String, String>() {{
			put("test1", "123");
			put("test2", "123");
		}});
	}
}
