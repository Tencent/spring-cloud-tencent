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

package com.tencent.tsf.gateway.core;

import java.net.URI;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link TsfGatewayRequest}.
 *
 * @author Haotian Zhang
 */
public class TsfGatewayRequestTest {

	@Test
	public void testTsfGatewayRequest() {
		TsfGatewayRequest tsfGatewayRequest = new TsfGatewayRequest();
		tsfGatewayRequest.setUri(URI.create("http://localhost:8080/test"));
		tsfGatewayRequest.setMethod("GET");
		tsfGatewayRequest.setHeaders(new HashMap<>());
		tsfGatewayRequest.setRequestHeaders(new HashMap<>());
		tsfGatewayRequest.putRequestHeader("request", "test");
		tsfGatewayRequest.setParameterMap(new HashMap<>());
		tsfGatewayRequest.setCookieMap(new HashMap<>());
		tsfGatewayRequest.putCookie("cookie", "test");

		assertThat(tsfGatewayRequest.getUri().toString()).isEqualTo("http://localhost:8080/test");
		assertThat(tsfGatewayRequest.getMethod()).isEqualTo("GET");
		assertThat(tsfGatewayRequest.getHeaders()).isNotNull();
		assertThat(tsfGatewayRequest.getHeader("test")).isNull();
		assertThat(tsfGatewayRequest.getRequestHeaders()).isNotNull();
		assertThat(tsfGatewayRequest.getRequestHeader("request")).isEqualTo("test");
		assertThat(tsfGatewayRequest.getParameterMap()).isNotNull();
		assertThat(tsfGatewayRequest.getCookieMap()).isNotNull();
		assertThat(tsfGatewayRequest.getCookie("cookie")).isEqualTo("test");
		assertThat(tsfGatewayRequest.toString()).isNotBlank();
	}
}
