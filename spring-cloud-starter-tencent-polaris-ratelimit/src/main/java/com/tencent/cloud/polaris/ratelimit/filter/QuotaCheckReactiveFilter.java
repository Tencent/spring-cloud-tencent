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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.resolver.RateLimitRuleArgumentReactiveResolver;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLimitedFallback;
import com.tencent.cloud.polaris.ratelimit.utils.QuotaCheckUtils;
import com.tencent.cloud.polaris.ratelimit.utils.RateLimitUtils;
import com.tencent.polaris.api.pojo.RetStatus;
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

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

/**
 * Reactive filter to check quota.
 *
 * @author Haotian Zhang, lepdou, kaiy, cheese8
 */
public class QuotaCheckReactiveFilter implements WebFilter, Ordered {

	private static final Logger LOG = LoggerFactory.getLogger(QuotaCheckReactiveFilter.class);

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
		return OrderConstant.Server.Reactive.RATE_LIMIT_FILTER_ORDER;
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
							.write(polarisRateLimiterLimitedFallback.rejectTips()
									.getBytes(polarisRateLimiterLimitedFallback.charset()));
				}
				else {
					response.setRawStatusCode(polarisRateLimitProperties.getRejectHttpCode());
					response.getHeaders().setContentType(MediaType.TEXT_HTML);
					dataBuffer = response.bufferFactory().allocateBuffer()
							.write(rejectTips.getBytes(StandardCharsets.UTF_8));
				}
				response.getHeaders()
						.add(HeaderConstant.INTERNAL_CALLEE_RET_STATUS, RetStatus.RetFlowControl.getDesc());
				if (Objects.nonNull(quotaResponse.getActiveRule())) {
					try {
						String encodedActiveRuleName = URLEncoder.encode(
								quotaResponse.getActiveRule().getName().getValue(), UTF_8);
						response.getHeaders().add(HeaderConstant.INTERNAL_ACTIVE_RULE_NAME, encodedActiveRuleName);
					}
					catch (UnsupportedEncodingException e) {
						LOG.error("Cannot encode {} for header internal-callee-activerule.",
								quotaResponse.getActiveRule().getName().getValue(), e);
					}
				}
				return response.writeWith(Mono.just(dataBuffer));
			}
			// Unirate
			if (quotaResponse.getCode() == QuotaResultCode.QuotaResultOk && quotaResponse.getWaitMs() > 0) {
				LOG.debug("The request of [{}] will waiting for {}ms.", path, quotaResponse.getWaitMs());
				waitMs = quotaResponse.getWaitMs();
			}
		}
		catch (Throwable t) {
			// An exception occurs in the rate limiting API call,
			// which should not affect the call of the business process.
			LOG.error("fail to invoke getQuota, service is " + localService, t);
		}

		if (waitMs > 0) {
			return Mono.delay(Duration.ofMillis(waitMs)).flatMap(e -> chain.filter(exchange));
		}
		else {
			return chain.filter(exchange);
		}
	}

}
