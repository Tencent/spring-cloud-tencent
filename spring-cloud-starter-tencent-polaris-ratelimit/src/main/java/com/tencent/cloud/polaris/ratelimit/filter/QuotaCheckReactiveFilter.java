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

package com.tencent.cloud.polaris.ratelimit.filter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant;
import com.tencent.cloud.polaris.ratelimit.utils.QuotaCheckUtils;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * Reactive filter to check quota.
 *
 * @author Haotian Zhang
 */
public class QuotaCheckReactiveFilter implements WebFilter, Ordered {

	private static final Logger LOG = LoggerFactory
			.getLogger(QuotaCheckReactiveFilter.class);

	private final LimitAPI limitAPI;

	public QuotaCheckReactiveFilter(LimitAPI limitAPI) {
		this.limitAPI = limitAPI;
	}

	@Override
	public int getOrder() {
		return RateLimitConstant.FILTER_ORDER;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		// get metadata of current thread
		MetadataContext metadataContext = exchange
				.getAttribute(MetadataConstant.HeaderName.METADATA_CONTEXT);

		if (metadataContext == null) {
			metadataContext = MetadataContextHolder.get();
		}

		String localNamespace = metadataContext
				.getSystemMetadata(MetadataConstant.SystemMetadataKey.LOCAL_NAMESPACE);
		String localService = metadataContext
				.getSystemMetadata(MetadataConstant.SystemMetadataKey.LOCAL_SERVICE);

		// TODO Get path
		String method = metadataContext
				.getSystemMetadata(MetadataConstant.SystemMetadataKey.LOCAL_PATH);
		Map<String, String> labels = null;
		if (StringUtils.isNotBlank(method)) {
			labels = new HashMap<>();
			labels.put("method", method);
		}

		try {
			QuotaResponse quotaResponse = QuotaCheckUtils.getQuota(limitAPI,
					localNamespace, localService, 1, labels, null);
			if (quotaResponse.getCode() == QuotaResultCode.QuotaResultLimited) {
				ServerHttpResponse response = exchange.getResponse();
				response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
				response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
				DataBuffer dataBuffer = response.bufferFactory().allocateBuffer().write(
						(RateLimitConstant.QUOTA_LIMITED_INFO + quotaResponse.getInfo())
								.getBytes(StandardCharsets.UTF_8));
				return response.writeWith(Mono.just(dataBuffer));
			}
		}
		catch (Throwable t) {
			// 限流API调用出现异常，不应该影响业务流程的调用
			LOG.error("fail to invoke getQuota, service is " + localService, t);
		}

		return chain.filter(exchange);
	}

}
