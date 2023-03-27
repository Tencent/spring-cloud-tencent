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
 *
 */

package com.tencent.cloud.polaris.ratelimit.filter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant;
import com.tencent.cloud.polaris.ratelimit.resolver.RateLimitRuleArgumentReactiveResolver;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLimitedFallback;
import com.tencent.cloud.polaris.ratelimit.utils.QuotaCheckUtils;
import com.tencent.cloud.polaris.ratelimit.utils.RateLimitUtils;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.Argument;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * Reactive filter to check quota.
 *
 * @author Haotian Zhang, lepdou, cheese8, kaiy
 */
public class QuotaCheckReactiveFilter implements WebFilter, Ordered {

	private static final Logger LOGGER = LoggerFactory.getLogger(QuotaCheckReactiveFilter.class);

	private final LimitAPI limitAPI;

	private final PolarisRateLimitProperties polarisRateLimitProperties;

	private final RateLimitRuleArgumentReactiveResolver rateLimitRuleArgumentResolver;

	private final PolarisRateLimiterLimitedFallback polarisRateLimiterLimitedFallback;


	private String rejectTips;

	public QuotaCheckReactiveFilter(LimitAPI limitAPI,
									PolarisRateLimitProperties polarisRateLimitProperties,
									RateLimitRuleArgumentReactiveResolver rateLimitRuleArgumentResolver,
									@Nullable PolarisRateLimiterLimitedFallback polarisRateLimiterLimitedFallback) {
		this.limitAPI = limitAPI;
		this.polarisRateLimitProperties = polarisRateLimitProperties;
		this.rateLimitRuleArgumentResolver = rateLimitRuleArgumentResolver;
		this.polarisRateLimiterLimitedFallback = polarisRateLimiterLimitedFallback;
	}

	@PostConstruct
	public void init() {
		rejectTips = RateLimitUtils.getRejectTips(polarisRateLimitProperties);
	}

	@Override
	public int getOrder() {
		return RateLimitConstant.FILTER_ORDER;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String localNamespace = MetadataContext.LOCAL_NAMESPACE;
		String localService = MetadataContext.LOCAL_SERVICE;

		Set<Argument> arguments = rateLimitRuleArgumentResolver.getArguments(exchange, localNamespace, localService);
		long waitMs = -1;
		try {
			String path = exchange.getRequest().getURI().getPath();
			QuotaResponse quotaResponse = QuotaCheckUtils.getQuota(
					limitAPI, localNamespace, localService, 1, arguments, path);

			if (quotaResponse.getCode() == QuotaResultCode.QuotaResultLimited) {
				ServerHttpResponse response = exchange.getResponse();
				DataBuffer dataBuffer;
				if (!Objects.isNull(polarisRateLimiterLimitedFallback)) {
					response.setRawStatusCode(polarisRateLimiterLimitedFallback.rejectHttpCode());
					response.getHeaders().setContentType(polarisRateLimiterLimitedFallback.mediaType());
					dataBuffer = response.bufferFactory().allocateBuffer()
							.write(polarisRateLimiterLimitedFallback.rejectTips().getBytes(polarisRateLimiterLimitedFallback.charset()));
				}
				else {
					response.setRawStatusCode(polarisRateLimitProperties.getRejectHttpCode());
					response.getHeaders().setContentType(MediaType.TEXT_HTML);
					dataBuffer = response.bufferFactory().allocateBuffer()
							.write(rejectTips.getBytes(StandardCharsets.UTF_8));
				}
				return response.writeWith(Mono.just(dataBuffer));
			}
			// Unirate
			if (quotaResponse.getCode() == QuotaResultCode.QuotaResultOk && quotaResponse.getWaitMs() > 0) {
				LOGGER.debug("The request of [{}] will waiting for {}ms.", path, quotaResponse.getWaitMs());
				waitMs = quotaResponse.getWaitMs();
			}
		}
		catch (Throwable t) {
			// An exception occurs in the rate limiting API call,
			// which should not affect the call of the business process.
			LOGGER.error("fail to invoke getQuota, service is " + localService, t);
		}

		if (waitMs > 0) {
			return Mono.delay(Duration.ofMillis(waitMs)).flatMap(e -> chain.filter(exchange));
		}
		else {
			return chain.filter(exchange);
		}
	}

}
