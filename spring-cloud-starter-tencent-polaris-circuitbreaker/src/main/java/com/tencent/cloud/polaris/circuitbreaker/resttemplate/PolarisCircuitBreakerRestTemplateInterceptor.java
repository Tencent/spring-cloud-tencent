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
import org.springframework.web.client.RestTemplate;

import static com.tencent.cloud.rpc.enhancement.resttemplate.EnhancedRestTemplateReporter.HEADER_HAS_ERROR;

/**
 * PolarisCircuitBreakerRestTemplateInterceptor.
 *
 * @author sean yu
 */
public class PolarisCircuitBreakerRestTemplateInterceptor implements ClientHttpRequestInterceptor {

	private final PolarisCircuitBreakerRestTemplate polarisCircuitBreakerRestTemplate;

	private final ApplicationContext applicationContext;

	private final CircuitBreakerFactory circuitBreakerFactory;

	private final RestTemplate restTemplate;

	public PolarisCircuitBreakerRestTemplateInterceptor(
			PolarisCircuitBreakerRestTemplate polarisCircuitBreakerRestTemplate,
			ApplicationContext applicationContext,
			CircuitBreakerFactory circuitBreakerFactory,
			RestTemplate restTemplate
	) {
		this.polarisCircuitBreakerRestTemplate = polarisCircuitBreakerRestTemplate;
		this.applicationContext = applicationContext;
		this.circuitBreakerFactory = circuitBreakerFactory;
		this.restTemplate =  restTemplate;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		return circuitBreakerFactory.create(request.getURI().getHost() + "#" + request.getURI().getPath()).run(
				() -> {
					try {
						ClientHttpResponse response = execution.execute(request, body);
						// pre handle response error
						// EnhancedRestTemplateReporter always return true,
						// so we need to check header set by EnhancedRestTemplateReporter
						restTemplate.getErrorHandler().hasError(response);
						if (Boolean.parseBoolean(response.getHeaders().getFirst(HEADER_HAS_ERROR))) {
							restTemplate.getErrorHandler().handleError(request.getURI(), request.getMethod(), response);
						}
						return response;
					}
					catch (IOException e) {
						throw new IllegalStateException(e);
					}
				},
				t -> {
					if (StringUtils.hasText(polarisCircuitBreakerRestTemplate.fallback())) {
						CircuitBreakerStatus.FallbackInfo fallbackInfo = new CircuitBreakerStatus.FallbackInfo(200, null, polarisCircuitBreakerRestTemplate.fallback());
						return new PolarisCircuitBreakerHttpResponse(fallbackInfo);
					}
					if (!PolarisCircuitBreakerFallback.class.toGenericString().equals(polarisCircuitBreakerRestTemplate.fallbackClass().toGenericString())) {
						Method method = ReflectionUtils.findMethod(PolarisCircuitBreakerFallback.class, "fallback");
						PolarisCircuitBreakerFallback polarisCircuitBreakerFallback = applicationContext.getBean(polarisCircuitBreakerRestTemplate.fallbackClass());
						return (PolarisCircuitBreakerHttpResponse) ReflectionUtils.invokeMethod(method, polarisCircuitBreakerFallback);
					}
					if (t instanceof CallAbortedException) {
						CircuitBreakerStatus.FallbackInfo fallbackInfo = ((CallAbortedException) t).getFallbackInfo();
						if (fallbackInfo != null) {
							return new PolarisCircuitBreakerHttpResponse(fallbackInfo);
						}
					}
					throw new IllegalStateException(t);
				}
		);
	}

}
