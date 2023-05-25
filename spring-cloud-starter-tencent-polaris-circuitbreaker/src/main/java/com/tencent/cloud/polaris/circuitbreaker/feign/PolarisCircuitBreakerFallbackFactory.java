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

package com.tencent.cloud.polaris.circuitbreaker.feign;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.polaris.circuitbreaker.exception.FallbackWrapperException;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.Decoder;

import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * PolarisCircuitBreakerFallbackFactory.
 *
 * @author sean yu
 */
public class PolarisCircuitBreakerFallbackFactory implements FallbackFactory {

	private final Decoder decoder;

	public PolarisCircuitBreakerFallbackFactory(Decoder decoder) {
		this.decoder = decoder;
	}

	@Override
	public Object create(Throwable t) {
		return new DefaultFallback(t, decoder);
	}

	public class DefaultFallback {

		private final Throwable t;

		private final Decoder decoder;

		public DefaultFallback(Throwable t, Decoder decoder) {
			this.t = t;
			this.decoder = decoder;
		}

		public Object fallback(Method method) {
			if (t instanceof CallAbortedException) {
				CircuitBreakerStatus.FallbackInfo fallbackInfo = ((CallAbortedException) t).getFallbackInfo();
				if (fallbackInfo != null) {
					Response.Builder responseBuilder = Response.builder()
							.status(fallbackInfo.getCode());
					if (fallbackInfo.getHeaders() != null) {
						Map<String, Collection<String>> headers = new HashMap<>();
						fallbackInfo.getHeaders().forEach((k, v) -> headers.put(k, Collections.singleton(v)));
						responseBuilder.headers(headers);
					}
					if (fallbackInfo.getBody() != null) {
						responseBuilder.body(fallbackInfo.getBody(), StandardCharsets.UTF_8);
					}
					// Feign Response need a nonnull Request,
					// which is not important in fallback response (no real request),
					// so we create a fake one
					Request fakeRequest = Request.create(Request.HttpMethod.GET, "/", new HashMap<>(), Request.Body.empty(), new RequestTemplate());
					responseBuilder.request(fakeRequest);

					try (Response response = responseBuilder.build()) {
						return decoder.decode(response, method.getGenericReturnType());
					}
					catch (IOException e) {
						throw new FallbackWrapperException(e);
					}
				}
			}
			throw new FallbackWrapperException(t);
		}
	}
}
