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

import com.tencent.tsf.gateway.core.constant.TsfAlgType;
import org.junit.jupiter.api.Test;
import shade.polaris.org.apache.commons.codec.binary.Base64;
import shade.polaris.org.apache.commons.codec.digest.HmacUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link TsfSignUtil}.
 *
 * @author Haotian Zhang
 */
public class TsfSignUtilTest {

	private static final String TEST_NONCE = "test-nonce";
	private static final String TEST_SECRET_ID = "test-secret-id";
	private static final String TEST_SECRET_KEY = "test-secret-key";

	@Test
	public void testGenerateWithHmacMd5() {
		// Given
		String expectedDigestValue = TEST_NONCE + TEST_SECRET_ID + TEST_SECRET_KEY;
		byte[] expectedBytes = HmacUtils.hmacMd5(TEST_SECRET_KEY, expectedDigestValue);
		String expectedSignature = Base64.encodeBase64String(expectedBytes);

		// When
		String actualSignature = TsfSignUtil.generate(TEST_NONCE, TEST_SECRET_ID, TEST_SECRET_KEY, TsfAlgType.HMAC_MD5);

		// Then
		assertThat(actualSignature).isEqualTo(expectedSignature);
		assertThat(actualSignature).isNotEmpty();
	}

	@Test
	public void testGenerateWithHmacSha1() {
		// Given
		String expectedDigestValue = TEST_NONCE + TEST_SECRET_ID + TEST_SECRET_KEY;
		byte[] expectedBytes = HmacUtils.hmacSha1(TEST_SECRET_KEY, expectedDigestValue);
		String expectedSignature = Base64.encodeBase64String(expectedBytes);

		// When
		String actualSignature = TsfSignUtil.generate(TEST_NONCE, TEST_SECRET_ID, TEST_SECRET_KEY, TsfAlgType.HMAC_SHA_1);

		// Then
		assertThat(actualSignature).isEqualTo(expectedSignature);
		assertThat(actualSignature).isNotEmpty();
	}

	@Test
	public void testGenerateWithHmacSha256() {
		// Given
		String expectedDigestValue = TEST_NONCE + TEST_SECRET_ID + TEST_SECRET_KEY;
		byte[] expectedBytes = HmacUtils.hmacSha256(TEST_SECRET_KEY, expectedDigestValue);
		String expectedSignature = Base64.encodeBase64String(expectedBytes);

		// When
		String actualSignature = TsfSignUtil.generate(TEST_NONCE, TEST_SECRET_ID, TEST_SECRET_KEY, TsfAlgType.HMAC_SHA_256);

		// Then
		assertThat(actualSignature).isEqualTo(expectedSignature);
		assertThat(actualSignature).isNotEmpty();
	}

	@Test
	public void testGenerateWithHmacSha512() {
		// Given
		String expectedDigestValue = TEST_NONCE + TEST_SECRET_ID + TEST_SECRET_KEY;
		byte[] expectedBytes = HmacUtils.hmacSha512(TEST_SECRET_KEY, expectedDigestValue);
		String expectedSignature = Base64.encodeBase64String(expectedBytes);

		// When
		String actualSignature = TsfSignUtil.generate(TEST_NONCE, TEST_SECRET_ID, TEST_SECRET_KEY, TsfAlgType.HMAC_SHA_512);

		// Then
		assertThat(actualSignature).isEqualTo(expectedSignature);
		assertThat(actualSignature).isNotEmpty();
	}

	@Test
	public void testGenerateWithUnsupportedAlgorithm() {
		// When & Then
		assertThatThrownBy(() -> TsfSignUtil.generate(TEST_NONCE, TEST_SECRET_ID, TEST_SECRET_KEY, TsfAlgType.HMAC_SM3))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("不支持的鉴权算法: HMAC_SM3");
	}

	@Test
	public void testGenerateWithEmptyParameters() {
		// Given
		String emptyNonce = "";
		String emptySecretId = "";
		String emptySecretKey = "";

		// When & Then
		assertThatThrownBy(() -> TsfSignUtil.generate(emptyNonce, emptySecretId, emptySecretKey, TsfAlgType.HMAC_MD5))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testGenerateConsistency() {
		// Given - Same input should produce same output
		String nonce = "consistent-nonce";
		String secretId = "consistent-secret-id";
		String secretKey = "consistent-secret-key";

		// When
		String signature1 = TsfSignUtil.generate(nonce, secretId, secretKey, TsfAlgType.HMAC_SHA_256);
		String signature2 = TsfSignUtil.generate(nonce, secretId, secretKey, TsfAlgType.HMAC_SHA_256);

		// Then
		assertThat(signature1).isEqualTo(signature2);
	}

	@Test
	public void testGenerateDifferentAlgorithmsProduceDifferentResults() {
		// When
		String md5Signature = TsfSignUtil.generate(TEST_NONCE, TEST_SECRET_ID, TEST_SECRET_KEY, TsfAlgType.HMAC_MD5);
		String sha1Signature = TsfSignUtil.generate(TEST_NONCE, TEST_SECRET_ID, TEST_SECRET_KEY, TsfAlgType.HMAC_SHA_1);
		String sha256Signature = TsfSignUtil.generate(TEST_NONCE, TEST_SECRET_ID, TEST_SECRET_KEY, TsfAlgType.HMAC_SHA_256);
		String sha512Signature = TsfSignUtil.generate(TEST_NONCE, TEST_SECRET_ID, TEST_SECRET_KEY, TsfAlgType.HMAC_SHA_512);

		// Then - Different algorithms should produce different signatures
		assertThat(md5Signature).isNotEqualTo(sha1Signature);
		assertThat(md5Signature).isNotEqualTo(sha256Signature);
		assertThat(md5Signature).isNotEqualTo(sha512Signature);
		assertThat(sha1Signature).isNotEqualTo(sha256Signature);
		assertThat(sha1Signature).isNotEqualTo(sha512Signature);
		assertThat(sha256Signature).isNotEqualTo(sha512Signature);
	}

	@Test
	public void testGenerateWithSpecialCharacters() {
		// Given
		String specialNonce = "test-nonce-with-special-chars!@#$%^&*()";
		String specialSecretId = "special-chars-secret-id-with-symbols";
		String specialSecretKey = "secret-key-with-special-characters";

		// When
		String signature = TsfSignUtil.generate(specialNonce, specialSecretId, specialSecretKey, TsfAlgType.HMAC_SHA_256);

		// Then
		assertThat(signature).isNotNull();
		assertThat(signature).isNotEmpty();

		// Verify signature result with special characters
		String expectedDigestValue = specialNonce + specialSecretId + specialSecretKey;
		byte[] expectedBytes = HmacUtils.hmacSha256(specialSecretKey, expectedDigestValue);
		String expectedSignature = Base64.encodeBase64String(expectedBytes);
		assertThat(signature).isEqualTo(expectedSignature);
	}

	@Test
	public void testGenerateSignatureFormat() {
		// When
		String signature = TsfSignUtil.generate(TEST_NONCE, TEST_SECRET_ID, TEST_SECRET_KEY, TsfAlgType.HMAC_SHA_256);

		// Then - Verify Base64 encoding format
		assertThat(signature).matches("^[A-Za-z0-9+/]*={0,2}$"); // Base64 format regex
		assertThat(Base64.isBase64(signature)).isTrue();
	}
}
