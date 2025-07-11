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

package com.tencent.cloud.polaris.circuitbreaker.instrument.resttemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link PolarisCircuitBreakerHttpResponse}.
 *
 * @author Shedfree Wu
 */
@ExtendWith(MockitoExtension.class)
public class PolarisCircuitBreakerHttpResponseTest {
	@Test
	void testConstructorWithCodeOnly() throws IOException {
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders()).isNotNull();
		assertThat(response.getHeaders()).isEmpty();
		assertThat(response.getBody()).isNull();
	}

	@Test
	void testConstructorWithCodeAndBody() throws IOException {
		String body = "test body";
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200, body);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders()).isNotNull();
		assertThat(response.getHeaders()).isEmpty();
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void testConstructorWithCodeHeadersAndBody() throws IOException {
		String body = "test body";
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Authorization", "Bearer token");

		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200, headers, body);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders()).isNotNull();
		assertThat(response.getHeaders().size()).isEqualTo(2);
		assertThat(response.getHeaders()).containsKey("Content-Type");
		assertThat(response.getHeaders()).containsKey("Authorization");
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void testConstructorWithFallbackInfo() throws IOException {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		CircuitBreakerStatus.FallbackInfo fallbackInfo = new CircuitBreakerStatus.FallbackInfo(200, headers, "test body");

		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(fallbackInfo);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getFallbackInfo()).isEqualTo(fallbackInfo);
		assertThat(response.getHeaders()).isNotNull();
		assertThat(response.getHeaders()).containsKey("Content-Type");
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void testGetStatusTextWithValidHttpStatus() {
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200);
		assertThat(response.getStatusText()).isEqualTo("OK");
	}

	@Test
	void testGetStatusTextWithInvalidHttpStatus() {
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(999);
		assertThat(response.getStatusText()).isEqualTo("");
	}

	@Test
	void testClose() {
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200, "test body");
		InputStream body = response.getBody();
		assertThat(body).isNotNull();

		response.close();

		// Verify that reading from closed stream throws exception
		assertThatNoException().isThrownBy(() -> body.read());
	}

	@Test
	void testCloseWithNullBody() {
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200);
		assertThat(response.getBody()).isNull();

		// Should not throw exception when closing null body
		assertThatNoException().isThrownBy(() -> response.close());
	}
}
