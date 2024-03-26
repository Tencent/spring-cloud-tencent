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

package com.tencent.cloud.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utils for {@link UrlUtils}.
 *
 * @author Shedfree Wu
 */
public class UrlUtilsTest {

	@Test
	public void testEncodeDecode1() {
		String expectEncodeValue = "a%2Fb";
		String origin = "a/b";
		String encode1 = UrlUtils.encode(origin);
		assertThat(expectEncodeValue).isEqualTo(encode1);
		// encode twice is different
		String encode2 = UrlUtils.encode(encode1);
		assertThat(encode1).isNotEqualTo(encode2);
		// test decode
		assertThat(origin).isEqualTo(UrlUtils.decode(encode1));
	}

	@Test
	public void testEncodeDecode2() {

		String origin = null;
		String encode1 = UrlUtils.encode(origin);
		assertThat(encode1).isNull();

		origin = "";
		encode1 = UrlUtils.encode(origin);
		assertThat(encode1).isEqualTo(origin);
	}

	@Test
	public void testError() {
		String origin = "a/b";
		String encode = UrlUtils.encode(origin, "error-enc");
		assertThat(encode).isEqualTo(origin);

		encode = "a%2Fb";
		String decode = UrlUtils.decode(encode, "error-enc");
		assertThat(decode).isEqualTo(encode);
	}

}
