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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ZuulFilterUtils;
import com.tencent.cloud.polaris.circuitbreaker.PolarisCircuitBreaker;
import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisCircuitBreakerHttpResponse;
import com.tencent.cloud.polaris.circuitbreaker.util.PolarisCircuitBreakerUtils;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static com.tencent.cloud.common.constant.ContextConstant.Zuul.POLARIS_CIRCUIT_BREAKER;
import static com.tencent.cloud.common.constant.ContextConstant.Zuul.POLARIS_IS_IN_ROUTING_STATE;
import static com.tencent.cloud.common.constant.ContextConstant.Zuul.POLARIS_PRE_ROUTE_TIME;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * Polaris circuit breaker implement in Zuul.
 *
 * @author Haotian Zhang
 */
public class PolarisCircuitBreakerZuulFilter extends ZuulFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisCircuitBreakerZuulFilter.class);

	private final CircuitBreakerFactory circuitBreakerFactory;

	private final PolarisZuulFallbackFactory polarisZuulFallbackFactory;

	private final Environment environment;

	public PolarisCircuitBreakerZuulFilter(
			CircuitBreakerFactory circuitBreakerFactory,
			PolarisZuulFallbackFactory polarisZuulFallbackFactory,
			Environment environment) {
		this.circuitBreakerFactory = circuitBreakerFactory;
		this.polarisZuulFallbackFactory = polarisZuulFallbackFactory;
		this.environment = environment;
	}

	@Override
	public String filterType() {
		return PRE_TYPE;
	}

	/**
	 * ServiceId is set after PreDecorationFilter.
	 *
	 * @return filter order
	 */
	@Override
	public int filterOrder() {
		return PRE_DECORATION_FILTER_ORDER + 2;
	}

	@Override
	public boolean shouldFilter() {
		String enabled = environment.getProperty("spring.cloud.polaris.circuitbreaker.enabled");
		return StringUtils.isEmpty(enabled) || enabled.equals("true");
	}

	@Override
	public Object run() throws ZuulException {
		RequestContext context = RequestContext.getCurrentContext();
		String serviceId = ZuulFilterUtils.getServiceId(context);
		String path = ZuulFilterUtils.getPath(context);
		String circuitName = "".equals(path) ?
				MetadataContext.LOCAL_NAMESPACE + "#" + serviceId :
				MetadataContext.LOCAL_NAMESPACE + "#" + serviceId + "#" + path;
		CircuitBreaker circuitBreaker = circuitBreakerFactory.create(circuitName);
		if (circuitBreaker instanceof PolarisCircuitBreaker) {
			PolarisCircuitBreaker polarisCircuitBreaker = (PolarisCircuitBreaker) circuitBreaker;
			context.set(POLARIS_CIRCUIT_BREAKER, circuitBreaker);
			try {
				polarisCircuitBreaker.acquirePermission();
			}
			catch (CallAbortedException exception) {
				FallbackProvider fallbackProvider = polarisZuulFallbackFactory.getFallbackProvider(serviceId);
				ClientHttpResponse clientHttpResponse;
				if (fallbackProvider != null) {
					clientHttpResponse = fallbackProvider.fallbackResponse(serviceId, exception);
				}
				else if (exception.getFallbackInfo() != null) {
					clientHttpResponse = new PolarisCircuitBreakerHttpResponse(exception.getFallbackInfo());
				}
				else {
					throw new IllegalStateException(exception);
				}
				try {
					context.setSendZuulResponse(false);
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
					LOGGER.debug("PolarisCircuitBreaker CallAbortedException: {}", exception.getMessage());
					PolarisCircuitBreakerUtils.reportStatus(polarisCircuitBreaker.getConsumerAPI(), polarisCircuitBreaker.getConf(), exception);
				}
				catch (IOException e) {
					LOGGER.error("Return circuit breaker fallback info failed: {}", e.getMessage());
					throw new IllegalStateException(e);
				}
			}
			context.set(POLARIS_PRE_ROUTE_TIME, Long.valueOf(System.currentTimeMillis()));
			context.set(POLARIS_IS_IN_ROUTING_STATE, true);
		}
		return null;
	}
}
