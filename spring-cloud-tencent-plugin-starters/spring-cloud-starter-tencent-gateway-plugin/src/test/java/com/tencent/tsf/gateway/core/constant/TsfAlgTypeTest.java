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

package com.tencent.tsf.gateway.core.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link TsfAlgType}.
 *
 * @author Haotian Zhang
 */
public class TsfAlgTypeTest {

	@Test
	public void testTsfAlgType() {
		assertThat(TsfAlgType.getSecurityCode("0")).isEqualTo(TsfAlgType.HMAC_MD5);
		assertThat(TsfAlgType.getSecurityCode("1")).isEqualTo(TsfAlgType.HMAC_SHA_1);
		assertThat(TsfAlgType.getSecurityCode("2")).isEqualTo(TsfAlgType.HMAC_SHA_256);
		assertThat(TsfAlgType.getSecurityCode("3")).isEqualTo(TsfAlgType.HMAC_SHA_512);
		assertThat(TsfAlgType.getSecurityCode("4")).isEqualTo(TsfAlgType.HMAC_SM3);
		assertThat(TsfAlgType.getSecurityCode("test")).isNull();

		assertThat(TsfAlgType.HMAC_MD5.getAlg()).isEqualTo("0");
		assertThat(TsfAlgType.HMAC_SHA_1.getAlg()).isEqualTo("1");
		assertThat(TsfAlgType.HMAC_SHA_256.getAlg()).isEqualTo("2");
		assertThat(TsfAlgType.HMAC_SHA_512.getAlg()).isEqualTo("3");
		assertThat(TsfAlgType.HMAC_SM3.getAlg()).isEqualTo("4");

		TsfAlgType.HMAC_MD5.setAlg("00");
		assertThat(TsfAlgType.HMAC_MD5.getAlg()).isEqualTo("00");
	}
}
