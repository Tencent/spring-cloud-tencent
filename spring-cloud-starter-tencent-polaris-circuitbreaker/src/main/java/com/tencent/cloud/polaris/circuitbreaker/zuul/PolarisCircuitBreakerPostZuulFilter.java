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

package com.tencent.cloud.polaris.circuitbreaker.zuul;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.util.ZuulFilterUtils;
import com.tencent.cloud.polaris.circuitbreaker.PolarisCircuitBreaker;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;

import static com.tencent.cloud.common.constant.ContextConstant.Zuul.POLARIS_CIRCUIT_BREAKER;
import static com.tencent.cloud.common.constant.ContextConstant.Zuul.POLARIS_PRE_ROUTE_TIME;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;

/**
 * Polaris circuit breaker post-processing. Including reporting.
 *
 * @author Haotian Zhang
 */
public class PolarisCircuitBreakerPostZuulFilter extends ZuulFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisCircuitBreakerPostZuulFilter.class);

	private final PolarisZuulFallbackFactory polarisZuulFallbackFactory;

	private final Environment environment;

	public PolarisCircuitBreakerPostZuulFilter(PolarisZuulFallbackFactory polarisZuulFallbackFactory,
			Environment environment) {
		this.polarisZuulFallbackFactory = polarisZuulFallbackFactory;
		this.environment = environment;
	}

	@Override
	public String filterType() {
		return POST_TYPE;
	}

	@Override
	public int filterOrder() {
		return OrderConstant.Client.Zuul.CIRCUIT_BREAKER_POST_FILTER_ORDER;
	}

	@Override
	public boolean shouldFilter() {
		String enabled = environment.getProperty("spring.cloud.polaris.circuitbreaker.enabled");
		return StringUtils.isEmpty(enabled) || enabled.equals("true");
	}

	@Override
	public Object run() throws ZuulException {
		RequestContext context = RequestContext.getCurrentContext();

		HttpServletResponse response = context.getResponse();
		HttpStatus status = HttpStatus.resolve(response.getStatus());

		Object polarisCircuitBreakerObject = context.get(POLARIS_CIRCUIT_BREAKER);
		Object startTimeMilliObject = context.get(POLARIS_PRE_ROUTE_TIME);
		if (polarisCircuitBreakerObject != null && polarisCircuitBreakerObject instanceof PolarisCircuitBreaker
				&& startTimeMilliObject != null && startTimeMilliObject instanceof Long) {
			PolarisCircuitBreaker polarisCircuitBreaker = (PolarisCircuitBreaker) polarisCircuitBreakerObject;
			Long startTimeMilli = (Long) startTimeMilliObject;
			long delay = System.currentTimeMillis() - startTimeMilli;
			InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
			responseContext.setDuration(delay);
			responseContext.setDurationUnit(TimeUnit.MILLISECONDS);

			if (status != null && status.is5xxServerError()) {
				Throwable throwable = new CircuitBreakerStatusCodeException(status);
				responseContext.setError(throwable);

				// fallback if FallbackProvider is implemented.
				String serviceId = ZuulFilterUtils.getServiceId(context);
				FallbackProvider fallbackProvider = polarisZuulFallbackFactory.getFallbackProvider(serviceId);
				if (fallbackProvider != null) {
					ClientHttpResponse clientHttpResponse = fallbackProvider.fallbackResponse(serviceId, throwable);
					try {
						// set status code
						context.setResponseStatusCode(clientHttpResponse.getRawStatusCode());
						// set headers
						HttpHeaders headers = clientHttpResponse.getHeaders();
						for (String key : headers.keySet()) {
							List<String> values = headers.get(key);
							if (!CollectionUtils.isEmpty(values)) {
								for (String value : values) {
									context.addZuulResponseHeader(key, value);
								}
							}
						}
						// set body
						context.getResponse().setCharacterEncoding("UTF-8");
						context.setResponseBody(IOUtils.toString(clientHttpResponse.getBody(), StandardCharsets.UTF_8));
					}
					catch (Exception e) {
						LOGGER.error("error filter exception", e);
					}
				}
			}

			if (responseContext.getError() == null) {
				polarisCircuitBreaker.onSuccess(responseContext);
			}
			else {
				polarisCircuitBreaker.onError(responseContext);
			}
		}
		return null;
	}

	public class CircuitBreakerStatusCodeException extends HttpStatusCodeException {

		public CircuitBreakerStatusCodeException(HttpStatus statusCode) {
			super(statusCode);
		}

	}
}
