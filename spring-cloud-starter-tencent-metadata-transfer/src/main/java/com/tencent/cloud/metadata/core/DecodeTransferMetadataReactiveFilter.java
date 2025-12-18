/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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
import java.util.concurrent.atomic.AtomicReference;

import com.tencent.cloud.common.async.PolarisAsyncProperties;
import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.common.util.TsfTagUtils;
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

	private final PolarisAsyncProperties polarisAsyncProperties;

	public DecodeTransferMetadataReactiveFilter(PolarisAsyncProperties polarisAsyncProperties) {
		this.polarisAsyncProperties = polarisAsyncProperties;
	}

	@Override
	public int getOrder() {
		return OrderConstant.Server.Reactive.DECODE_TRANSFER_METADATA_FILTER_ORDER;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
		// Get metadata string from http header.
		ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();

		Map<String, String> mergedTransitiveMetadata = new HashMap<>();
		Map<String, String> mergedDisposableMetadata = new HashMap<>();
		Map<String, String> mergedApplicationMetadata = new HashMap<>();
		// some tsf headers need to change to polaris header
		Map<String, String> addHeaders = new HashMap<>();
		AtomicReference<String> callerIp = new AtomicReference<>("");

		TsfTagUtils.updateTsfMetadata(mergedTransitiveMetadata, mergedDisposableMetadata,
				mergedApplicationMetadata, addHeaders, callerIp,
				serverHttpRequest.getHeaders().getFirst(MetadataConstant.HeaderName.TSF_TAGS),
				serverHttpRequest.getHeaders().getFirst(MetadataConstant.HeaderName.TSF_SYSTEM_TAG),
				serverHttpRequest.getHeaders().getFirst(MetadataConstant.HeaderName.TSF_METADATA));

		// transitive metadata
		// from specific header
		Map<String, String> internalTransitiveMetadata = getInternalMetadata(serverHttpRequest, CUSTOM_METADATA);
		// from header with specific prefix
		Map<String, String> customTransitiveMetadata = CustomTransitiveMetadataResolver.resolve(serverWebExchange);
		mergedTransitiveMetadata.putAll(internalTransitiveMetadata);
		mergedTransitiveMetadata.putAll(customTransitiveMetadata);

		// disposable metadata
		// from specific header
		Map<String, String> internalDisposableMetadata = getInternalMetadata(serverHttpRequest, CUSTOM_DISPOSABLE_METADATA);
		mergedDisposableMetadata.putAll(internalDisposableMetadata);

		// application metadata
		Map<String, String> internalApplicationMetadata = getInternalMetadata(serverHttpRequest, APPLICATION_METADATA);
		mergedApplicationMetadata.putAll(internalApplicationMetadata);

		if (StringUtils.isNotBlank(mergedApplicationMetadata.get(LOCAL_IP))) {
			callerIp.set(mergedApplicationMetadata.get(LOCAL_IP));
		}
		// add headers
		serverHttpRequest = serverHttpRequest.mutate().headers(httpHeaders -> {
			for (Map.Entry<String, String> entry : addHeaders.entrySet()) {
				httpHeaders.add(entry.getKey(), entry.getValue());
			}
		}).build();
		// message metadata
		ReactiveMetadataProvider callerMessageMetadataProvider = new ReactiveMetadataProvider(serverHttpRequest,
				callerIp.get(), polarisAsyncProperties.getEnabled());

		MetadataContextHolder.init(mergedTransitiveMetadata, mergedDisposableMetadata, mergedApplicationMetadata, callerMessageMetadataProvider);

		// Save to ServerWebExchange.
		serverWebExchange.getAttributes().put(
				MetadataConstant.HeaderName.METADATA_CONTEXT,
				MetadataContextHolder.get());

		String targetNamespace = serverWebExchange.getRequest().getHeaders()
				.getFirst(MetadataConstant.HeaderName.NAMESPACE);
		// Compatible with TSF
		if (StringUtils.isBlank(targetNamespace)) {
			targetNamespace = serverWebExchange.getRequest().getHeaders()
					.getFirst(MetadataConstant.HeaderName.TSF_NAMESPACE_ID);
		}

		if (StringUtils.isNotBlank(targetNamespace)) {
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
					MetadataConstant.POLARIS_TARGET_NAMESPACE, targetNamespace);
		}
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
