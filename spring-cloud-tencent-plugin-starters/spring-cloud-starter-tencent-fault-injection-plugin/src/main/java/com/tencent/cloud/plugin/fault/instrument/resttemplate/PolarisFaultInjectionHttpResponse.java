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

package com.tencent.cloud.plugin.fault.instrument.resttemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import org.jetbrains.annotations.NotNull;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

/**
 * PolarisFaultInjectionHttpResponse.
 *
 * @author Haotian Zhang
 */
public class PolarisFaultInjectionHttpResponse implements ClientHttpResponse {

	private final CircuitBreakerStatus.FallbackInfo fallbackInfo;

	private HttpHeaders headers = new HttpHeaders();

	private InputStream body;

	public PolarisFaultInjectionHttpResponse(int code) {
		this(new CircuitBreakerStatus.FallbackInfo(code, null, null));
	}

	public PolarisFaultInjectionHttpResponse(int code, String body) {
		this(new CircuitBreakerStatus.FallbackInfo(code, null, body));
	}

	public PolarisFaultInjectionHttpResponse(int code, Map<String, String> headers, String body) {
		this(new CircuitBreakerStatus.FallbackInfo(code, headers, body));
	}

	public PolarisFaultInjectionHttpResponse(CircuitBreakerStatus.FallbackInfo fallbackInfo) {
		this.fallbackInfo = fallbackInfo;
		if (fallbackInfo.getHeaders() != null) {
			fallbackInfo.getHeaders().forEach(headers::add);
		}
		if (fallbackInfo.getBody() != null) {
			body = new ByteArrayInputStream(fallbackInfo.getBody().getBytes());
		}
	}

	@NotNull
	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(fallbackInfo.getCode());
	}

	@Override
	public int getRawStatusCode() throws IOException {
		return fallbackInfo.getCode();
	}

	@NotNull
	@Override
	public final String getStatusText() {
		HttpStatus status = HttpStatus.resolve(getStatusCode().value());
		return (status != null ? status.getReasonPhrase() : "");
	}

	@Override
	public final void close() {
		if (this.body != null) {
			try {
				this.body.close();
			}
			catch (IOException e) {
				// Ignore exception on close...
			}
		}
	}

	@Override
	public final InputStream getBody() {
		return this.body;
	}

	@Override
	public final HttpHeaders getHeaders() {
		return this.headers;
	}

	public CircuitBreakerStatus.FallbackInfo getFallbackInfo() {
		return this.fallbackInfo;
	}
}
