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
 */

package com.tencent.cloud.polaris.circuitbreaker.resttemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.tencent.polaris.api.pojo.CircuitBreakerStatus;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpResponse;

/**
 * PolarisCircuitBreakerHttpResponse.
 *
 * @author sean yu
 */
public class PolarisCircuitBreakerHttpResponse extends AbstractClientHttpResponse {

	private final CircuitBreakerStatus.FallbackInfo fallbackInfo;

	private HttpHeaders headers = new HttpHeaders();

	private InputStream body;

	public PolarisCircuitBreakerHttpResponse(int code) {
		this(new CircuitBreakerStatus.FallbackInfo(code, null, null));
	}

	public PolarisCircuitBreakerHttpResponse(int code, String body) {
		this(new CircuitBreakerStatus.FallbackInfo(code, null, body));
	}

	public PolarisCircuitBreakerHttpResponse(int code, Map<String, String> headers, String body) {
		this(new CircuitBreakerStatus.FallbackInfo(code, headers, body));
	}

	PolarisCircuitBreakerHttpResponse(CircuitBreakerStatus.FallbackInfo fallbackInfo) {
		this.fallbackInfo = fallbackInfo;
		if (fallbackInfo.getHeaders() != null) {
			fallbackInfo.getHeaders().forEach(headers::add);
		}
		if (fallbackInfo.getBody() != null) {
			body = new ByteArrayInputStream(fallbackInfo.getBody().getBytes());
		}
	}

	@Override
	public final int getRawStatusCode() {
		return fallbackInfo.getCode();
	}

	@Override
	public final String getStatusText() {
		HttpStatus status = HttpStatus.resolve(getRawStatusCode());
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
