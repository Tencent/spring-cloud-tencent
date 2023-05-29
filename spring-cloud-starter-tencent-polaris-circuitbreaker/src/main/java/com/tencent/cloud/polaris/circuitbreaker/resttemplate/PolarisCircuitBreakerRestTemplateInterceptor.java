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

import java.io.IOException;
import java.lang.reflect.Method;

import com.tencent.cloud.polaris.circuitbreaker.exception.FallbackWrapperException;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * PolarisCircuitBreakerRestTemplateInterceptor.
 *
 * @author sean yu
 */
public class PolarisCircuitBreakerRestTemplateInterceptor implements ClientHttpRequestInterceptor {

	private final PolarisCircuitBreaker polarisCircuitBreaker;

	private final ApplicationContext applicationContext;

	private final CircuitBreakerFactory circuitBreakerFactory;

	private final RestTemplate restTemplate;

	public PolarisCircuitBreakerRestTemplateInterceptor(
			PolarisCircuitBreaker polarisCircuitBreaker,
			ApplicationContext applicationContext,
			CircuitBreakerFactory circuitBreakerFactory,
			RestTemplate restTemplate
	) {
		this.polarisCircuitBreaker = polarisCircuitBreaker;
		this.applicationContext = applicationContext;
		this.circuitBreakerFactory = circuitBreakerFactory;
		this.restTemplate =  restTemplate;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		try {
			return circuitBreakerFactory.create(request.getURI().getHost() + "#" + request.getURI().getPath()).run(
					() -> {
						try {
							ClientHttpResponse response = execution.execute(request, body);
							ResponseErrorHandler errorHandler = restTemplate.getErrorHandler();
							if (errorHandler.hasError(response)) {
								errorHandler.handleError(request.getURI(), request.getMethod(), response);
							}
							return response;
						}
						catch (IOException e) {
							throw new IllegalStateException(e);
						}
					},
					t -> {
						if (StringUtils.hasText(polarisCircuitBreaker.fallback())) {
							CircuitBreakerStatus.FallbackInfo fallbackInfo = new CircuitBreakerStatus.FallbackInfo(200, null, polarisCircuitBreaker.fallback());
							return new PolarisCircuitBreakerHttpResponse(fallbackInfo);
						}
						if (!PolarisCircuitBreakerFallback.class.toGenericString().equals(polarisCircuitBreaker.fallbackClass().toGenericString())) {
							Method method = ReflectionUtils.findMethod(PolarisCircuitBreakerFallback.class, "fallback");
							PolarisCircuitBreakerFallback polarisCircuitBreakerFallback = applicationContext.getBean(polarisCircuitBreaker.fallbackClass());
							return (PolarisCircuitBreakerHttpResponse) ReflectionUtils.invokeMethod(method, polarisCircuitBreakerFallback);
						}
						if (t instanceof CallAbortedException) {
							CircuitBreakerStatus.FallbackInfo fallbackInfo = ((CallAbortedException) t).getFallbackInfo();
							if (fallbackInfo != null) {
								return new PolarisCircuitBreakerHttpResponse(fallbackInfo);
							}
						}
						throw new FallbackWrapperException(t);
					}
			);
		}
		catch (FallbackWrapperException e) {
			// unwrap And Rethrow
			Throwable underlyingException = e.getCause();
			if (underlyingException instanceof RuntimeException) {
				throw (RuntimeException) underlyingException;
			}
			throw new IllegalStateException(underlyingException);
		}

	}

}
