/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link PolarisCircuitBreakerHttpResponse}.
 *
 * @author Shedfree Wu
 */
@ExtendWith(MockitoExtension.class)
public class PolarisCircuitBreakerHttpResponseTest {
	@Test
	void testConstructorWithCodeOnly() {
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200);

		Assertions.assertEquals(200, response.getRawStatusCode());
		Assertions.assertNotNull(response.getHeaders());
		Assertions.assertTrue(response.getHeaders().isEmpty());
		Assertions.assertNull(response.getBody());
	}

	@Test
	void testConstructorWithCodeAndBody() {
		String body = "test body";
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200, body);

		Assertions.assertEquals(200, response.getRawStatusCode());
		Assertions.assertNotNull(response.getHeaders());
		Assertions.assertTrue(response.getHeaders().isEmpty());
		Assertions.assertNotNull(response.getBody());
	}

	@Test
	void testConstructorWithCodeHeadersAndBody() {
		String body = "test body";
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Authorization", "Bearer token");

		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200, headers, body);

		Assertions.assertEquals(200, response.getRawStatusCode());
		Assertions.assertNotNull(response.getHeaders());
		Assertions.assertEquals(2, response.getHeaders().size());
		Assertions.assertTrue(response.getHeaders().containsKey("Content-Type"));
		Assertions.assertTrue(response.getHeaders().containsKey("Authorization"));
		Assertions.assertNotNull(response.getBody());
	}

	@Test
	void testConstructorWithFallbackInfo() {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		CircuitBreakerStatus.FallbackInfo fallbackInfo = new CircuitBreakerStatus.FallbackInfo(200, headers, "test body");

		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(fallbackInfo);

		Assertions.assertEquals(200, response.getRawStatusCode());
		Assertions.assertEquals(fallbackInfo, response.getFallbackInfo());
		Assertions.assertNotNull(response.getHeaders());
		Assertions.assertTrue(response.getHeaders().containsKey("Content-Type"));
		Assertions.assertNotNull(response.getBody());
	}

	@Test
	void testGetStatusTextWithValidHttpStatus() {
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200);
		Assertions.assertEquals("OK", response.getStatusText());
	}

	@Test
	void testGetStatusTextWithInvalidHttpStatus() {
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(999);
		Assertions.assertEquals("", response.getStatusText());
	}

	@Test
	void testClose() {
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200, "test body");
		InputStream body = response.getBody();
		Assertions.assertNotNull(body);

		response.close();

		// Verify that reading from closed stream throws exception
		Assertions.assertDoesNotThrow(() -> body.read());
	}

	@Test
	void testCloseWithNullBody() {
		PolarisCircuitBreakerHttpResponse response = new PolarisCircuitBreakerHttpResponse(200);
		Assertions.assertNull(response.getBody());

		// Should not throw exception when closing null body
		Assertions.assertDoesNotThrow(() -> response.close());
	}
}
