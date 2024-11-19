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

package com.tencent.cloud.metadata.core;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.common.util.UrlUtils;
import com.tencent.cloud.metadata.provider.ReactiveMetadataProvider;
import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.APPLICATION_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;
import static com.tencent.polaris.metadata.core.constant.MetadataConstants.LOCAL_IP;

/**
 * Filter used for storing the metadata from upstream temporarily when web application is
 * REACTIVE.
 *
 * @author Haotian Zhang
 */
public class DecodeTransferMetadataReactiveFilter implements WebFilter, Ordered {

	private static final Logger LOG = LoggerFactory.getLogger(DecodeTransferMetadataReactiveFilter.class);

	@Override
	public int getOrder() {
		return OrderConstant.Server.Reactive.DECODE_TRANSFER_METADATA_FILTER_ORDER;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
		// Get metadata string from http header.
		ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();

		// transitive metadata
		// from specific header
		Map<String, String> internalTransitiveMetadata = getInternalMetadata(serverHttpRequest, CUSTOM_METADATA);
		// from header with specific prefix
		Map<String, String> customTransitiveMetadata = CustomTransitiveMetadataResolver.resolve(serverWebExchange);
		Map<String, String> mergedTransitiveMetadata = new HashMap<>();
		mergedTransitiveMetadata.putAll(internalTransitiveMetadata);
		mergedTransitiveMetadata.putAll(customTransitiveMetadata);

		// disposable metadata
		// from specific header
		Map<String, String> internalDisposableMetadata = getInternalMetadata(serverHttpRequest, CUSTOM_DISPOSABLE_METADATA);
		Map<String, String> mergedDisposableMetadata = new HashMap<>(internalDisposableMetadata);

		// application metadata
		Map<String, String> internalApplicationMetadata = getInternalMetadata(serverHttpRequest, APPLICATION_METADATA);
		Map<String, String> mergedApplicationMetadata = new HashMap<>(internalApplicationMetadata);

		String callerIp = "";
		if (StringUtils.isNotBlank(mergedApplicationMetadata.get(LOCAL_IP))) {
			callerIp = mergedApplicationMetadata.get(LOCAL_IP);
		}
		// message metadata
		ReactiveMetadataProvider callerMessageMetadataProvider = new ReactiveMetadataProvider(serverHttpRequest, callerIp);

		MetadataContextHolder.init(mergedTransitiveMetadata, mergedDisposableMetadata, mergedApplicationMetadata, callerMessageMetadataProvider);

		// Save to ServerWebExchange.
		serverWebExchange.getAttributes().put(
				MetadataConstant.HeaderName.METADATA_CONTEXT,
				MetadataContextHolder.get());

		TransHeadersTransfer.transfer(serverHttpRequest);
		return webFilterChain.filter(serverWebExchange)
				.doFinally((type) -> MetadataContextHolder.remove());
	}

	private Map<String, String> getInternalMetadata(ServerHttpRequest serverHttpRequest, String headerName) {
		HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
		String customMetadataStr = UrlUtils.decode(httpHeaders.getFirst(headerName));
		LOG.debug("Get upstream metadata string: {}", customMetadataStr);

		return JacksonUtils.deserialize2Map(customMetadataStr);
	}
}
