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

package com.tencent.cloud.polaris.circuitbreaker.common;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for ${@link PolarisResultToErrorCode}.
 *
 * @author Shedfree Wu
 */
@ExtendWith(MockitoExtension.class)
class PolarisResultToErrorCodeTest {

	private final PolarisResultToErrorCode converter = new PolarisResultToErrorCode();

	@Test
	void testOnSuccess() {
		assertThat(converter.onSuccess("any value")).isEqualTo(200);
	}

	@Test
	void testOnErrorWithWebClientResponseException() {
		// Given
		WebClientResponseException exception = WebClientResponseException.create(
				404, "Not Found", null, null, null);

		// When
		int errorCode = converter.onError(exception);

		// Then
		assertThat(errorCode).isEqualTo(404);
	}

	@Test
	void testOnErrorWithCircuitBreakerStatusCodeException() {
		// When
		int errorCode = converter.onError(new RuntimeException("test"));

		// Then
		assertThat(errorCode).isEqualTo(-1);
	}

	@Test
	void testOnErrorWithUnknownException() {
		// Given
		RuntimeException exception = new RuntimeException("Unknown error");

		// When
		int errorCode = converter.onError(exception);

		// Then
		assertThat(errorCode).isEqualTo(-1);
	}

	@Test
	void testCheckClassExist() throws Exception {
		// Given
		Method checkClassExist = PolarisResultToErrorCode.class.getDeclaredMethod("checkClassExist", String.class);
		checkClassExist.setAccessible(true);

		PolarisResultToErrorCode converter = new PolarisResultToErrorCode();

		// test exist class
		boolean result1 = (boolean) checkClassExist.invoke(converter, "java.lang.String");
		assertThat(result1).isTrue();


		// test not exist class
		boolean result2 = (boolean) checkClassExist.invoke(converter, "com.nonexistent.Class");
		assertThat(result2).isFalse();
	}
}
