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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link GzipUtil}.
 *
 * @author Haotian Zhang
 */
public class GzipUtilTest {

	@Test
	public void testGzip() {
		try {
			String testData = "1234567890";
			String compressed = GzipUtil.compressBase64Encode(testData, "UTF-8");
			String decompressed = GzipUtil.base64DecodeDecompress(compressed, "UTF-8");
			assertThat(decompressed).isEqualTo(testData);

			assertThatThrownBy(() -> {
				GzipUtil.compressBase64Encode(testData, "UTF-7");
			}).isInstanceOf(IOException.class);

			compressed = GzipUtil.compressBase64Encode(testData.getBytes(), "UTF-8");
			decompressed = GzipUtil.base64DecodeDecompress(compressed, "UTF-8");
			assertThat(decompressed).isEqualTo(testData);
			decompressed = GzipUtil.base64DecodeDecompress(compressed);
			assertThat(decompressed).isEqualTo(testData);
		}
		catch (IOException e) {
			fail("Gzip compress or decompress failed", e);
		}
	}
}
