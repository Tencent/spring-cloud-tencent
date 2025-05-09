/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.ratelimit.filter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import javax.annotation.PostConstruct;

import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLimitedFallback;
import com.tencent.cloud.polaris.ratelimit.utils.QuotaCheckUtils;
import com.tencent.cloud.polaris.ratelimit.utils.RateLimitUtils;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
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
import static org.springframework.core.io.buffer.DefaultDataBufferFactory.DEFAULT_INITIAL_CAPACITY;

/**
 * Reactive filter to check quota.
 *
 * @author Haotian Zhang, lepdou, kaiy, cheese8
 */
public class QuotaCheckReactiveFilter implements WebFilter, Ordered {

	private static final Logger LOG = LoggerFactory.getLogger(QuotaCheckReactiveFilter.class);

	private final LimitAPI limitAPI;

	private final AssemblyAPI assemblyAPI;

	private final PolarisRateLimitProperties polarisRateLimitProperties;

	private final PolarisRateLimiterLimitedFallback polarisRateLimiterLimitedFallback;

	private String rejectTips;

	public QuotaCheckReactiveFilter(LimitAPI limitAPI, AssemblyAPI assemblyAPI,
			PolarisRateLimitProperties polarisRateLimitProperties,
			@Nullable PolarisRateLimiterLimitedFallback polarisRateLimiterLimitedFallback) {
		this.limitAPI = limitAPI;
		this.assemblyAPI = assemblyAPI;
		this.polarisRateLimitProperties = polarisRateLimitProperties;
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

		long waitMs = -1;
		QuotaResponse quotaResponse = null;
		try {
			String path = exchange.getRequest().getURI().getPath();
			quotaResponse = QuotaCheckUtils.getQuota(limitAPI, localNamespace, localService, 1, path);

			if (quotaResponse.getCode() == QuotaResultCode.QuotaResultLimited) {
				LOG.info("block by ratelimit rule, uri:{}", exchange.getRequest().getURI());
				ServerHttpResponse response = exchange.getResponse();
				DataBuffer dataBuffer;
				if (Objects.nonNull(quotaResponse.getActiveRule())
						&& StringUtils.isNotBlank(quotaResponse.getActiveRule().getCustomResponse().getBody())) {
					response.setRawStatusCode(polarisRateLimitProperties.getRejectHttpCode());
					response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
					dataBuffer = response.bufferFactory().allocateBuffer(DEFAULT_INITIAL_CAPACITY)
							.write(quotaResponse.getActiveRule().getCustomResponse().getBody()
									.getBytes(StandardCharsets.UTF_8));
				}
				else if (!Objects.isNull(polarisRateLimiterLimitedFallback)) {
					response.setRawStatusCode(polarisRateLimiterLimitedFallback.rejectHttpCode());
					response.getHeaders().setContentType(polarisRateLimiterLimitedFallback.mediaType());
					dataBuffer = response.bufferFactory().allocateBuffer(DEFAULT_INITIAL_CAPACITY)
							.write(polarisRateLimiterLimitedFallback.rejectTips()
									.getBytes(polarisRateLimiterLimitedFallback.charset()));
				}
				else {
					response.setRawStatusCode(polarisRateLimitProperties.getRejectHttpCode());
					response.getHeaders().setContentType(MediaType.TEXT_HTML);
					dataBuffer = response.bufferFactory().allocateBuffer(DEFAULT_INITIAL_CAPACITY)
							.write(rejectTips.getBytes(StandardCharsets.UTF_8));
				}
				// set flow control to header
				response.getHeaders()
						.add(HeaderConstant.INTERNAL_CALLEE_RET_STATUS, RetStatus.RetFlowControl.getDesc());
				// set trace span
				RateLimitUtils.reportTrace(assemblyAPI, quotaResponse.getActiveRule().getId().getValue());
				if (Objects.nonNull(quotaResponse.getActiveRule())) {
					try {
						String encodedActiveRuleName = URLEncoder.encode(
								quotaResponse.getActiveRuleName(), UTF_8);
						response.getHeaders().add(HeaderConstant.INTERNAL_ACTIVE_RULE_NAME, encodedActiveRuleName);
					}
					catch (UnsupportedEncodingException e) {
						LOG.error("Cannot encode {} for header internal-callee-activerule.",
								quotaResponse.getActiveRuleName(), e);
					}
				}
				RateLimitUtils.release(quotaResponse);
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

		QuotaResponse finalQuotaResponse = quotaResponse;
		if (waitMs > 0) {
			return Mono.delay(Duration.ofMillis(waitMs))
					.flatMap(e -> chain.filter(exchange).doFinally((v) -> RateLimitUtils.release(finalQuotaResponse)));
		}
		else {
			return chain.filter(exchange).doFinally((v) -> RateLimitUtils.release(finalQuotaResponse));
		}
	}

}
