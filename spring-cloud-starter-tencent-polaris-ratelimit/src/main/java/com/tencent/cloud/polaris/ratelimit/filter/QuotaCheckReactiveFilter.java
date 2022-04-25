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
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelReactiveResolver;
import com.tencent.cloud.polaris.ratelimit.utils.QuotaCheckUtils;
import com.tencent.cloud.polaris.ratelimit.utils.RateLimitUtils;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import static com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant.LABEL_METHOD;

/**
 * Reactive filter to check quota.
 *
 * @author Haotian Zhang
 */
public class QuotaCheckReactiveFilter implements WebFilter, Ordered {

	private static final Logger LOG = LoggerFactory
			.getLogger(QuotaCheckReactiveFilter.class);

	private final LimitAPI limitAPI;

	private final PolarisRateLimiterLabelReactiveResolver labelResolver;

	private final PolarisRateLimitProperties polarisRateLimitProperties;

	private String rejectTips;

	public QuotaCheckReactiveFilter(LimitAPI limitAPI,
			PolarisRateLimiterLabelReactiveResolver labelResolver,
			PolarisRateLimitProperties polarisRateLimitProperties) {
		this.limitAPI = limitAPI;
		this.labelResolver = labelResolver;
		this.polarisRateLimitProperties = polarisRateLimitProperties;
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

		Map<String, String> labels = new HashMap<>();

		// add build in labels
		String path = exchange.getRequest().getURI().getPath();
		if (StringUtils.isNotBlank(path)) {
			labels.put(LABEL_METHOD, path);
		}

		// add custom labels
		if (labelResolver != null) {
			try {
				Map<String, String> customLabels = labelResolver.resolve(exchange);
				if (!CollectionUtils.isEmpty(customLabels)) {
					labels.putAll(customLabels);
				}
			}
			catch (Throwable e) {
				LOG.error("resolve custom label failed. resolver = {}",
						labelResolver.getClass().getName(), e);
			}
		}

		try {
			QuotaResponse quotaResponse = QuotaCheckUtils.getQuota(limitAPI,
					localNamespace, localService, 1, labels, null);

			if (quotaResponse.getCode() == QuotaResultCode.QuotaResultLimited) {
				ServerHttpResponse response = exchange.getResponse();
				response.setRawStatusCode(polarisRateLimitProperties.getRejectHttpCode());
				response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
				DataBuffer dataBuffer = response.bufferFactory().allocateBuffer()
						.write(rejectTips.getBytes(StandardCharsets.UTF_8));
				return response.writeWith(Mono.just(dataBuffer));
			}
		}
		catch (Throwable t) {
			// An exception occurs in the rate limiting API call,
			// which should not affect the call of the business process.
			LOG.error("fail to invoke getQuota, service is " + localService, t);
		}

		return chain.filter(exchange);
	}

}
