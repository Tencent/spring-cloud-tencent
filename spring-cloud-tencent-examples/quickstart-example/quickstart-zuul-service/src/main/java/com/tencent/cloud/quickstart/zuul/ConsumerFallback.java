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

package com.tencent.cloud.quickstart.zuul;


import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/**
 * Custom fallback.
 *
 * @author Haotian Zhang
 */
@Component
public class ConsumerFallback implements FallbackProvider {

	private static final Logger LOG = LoggerFactory.getLogger(ConsumerFallback.class);

	@Override
	public String getRoute() {
		return "polaris-circuitbreaker-callee-service";
	}

	@Override
	public ClientHttpResponse fallbackResponse(String route, Throwable cause) {
		if (cause != null && cause.getCause() != null) {
			LOG.error("Fallback is called.", cause);
		}

		return new ClientHttpResponse() {
			@Override
			public HttpStatus getStatusCode() {
				return HttpStatus.OK;
			}

			@Override
			public int getRawStatusCode() {
				return 200;
			}

			@Override
			public String getStatusText() {
				return "OK";
			}

			@Override
			public void close() {

			}

			@Override
			public InputStream getBody() {
				return new ByteArrayInputStream("zuul custom fallback".getBytes());
			}

			@Override
			public HttpHeaders getHeaders() {
				HttpHeaders httpHeaders = new HttpHeaders();
				httpHeaders.setContentType(MediaType.TEXT_PLAIN);
				return httpHeaders;
			}
		};
	}
}
