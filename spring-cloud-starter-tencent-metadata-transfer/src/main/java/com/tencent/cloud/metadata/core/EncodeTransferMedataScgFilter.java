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

package com.tencent.cloud.metadata.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;

/**
 * Scg filter used for writing metadata in HTTP request header.
 *
 * @author Haotian Zhang
 */
public class EncodeTransferMedataScgFilter implements GlobalFilter, Ordered {

	@Override
	public int getOrder() {
		return OrderConstant.Client.Scg.ENCODE_TRANSFER_METADATA_FILTER_ORDER;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// get request builder
		ServerHttpRequest.Builder builder = exchange.getRequest().mutate();

		// get metadata of current thread
		MetadataContext metadataContext = exchange.getAttribute(MetadataConstant.HeaderName.METADATA_CONTEXT);
		if (metadataContext == null) {
			metadataContext = MetadataContextHolder.get();
		}

		Map<String, String> customMetadata = metadataContext.getCustomMetadata();
		Map<String, String> disposableMetadata = metadataContext.getDisposableMetadata();

		this.buildMetadataHeader(builder, customMetadata, CUSTOM_METADATA);
		this.buildMetadataHeader(builder, disposableMetadata, CUSTOM_DISPOSABLE_METADATA);

		TransHeadersTransfer.transfer(exchange.getRequest());
		return chain.filter(exchange.mutate().request(builder.build()).build());
	}

	/**
	 * Set metadata into the request header for {@link ServerHttpRequest.Builder} .
	 * @param builder instance of {@link ServerHttpRequest.Builder}
	 * @param metadata metadata map .
	 * @param headerName target metadata http header name .
	 */
	private void buildMetadataHeader(ServerHttpRequest.Builder builder, Map<String, String> metadata, String headerName) {
		if (!CollectionUtils.isEmpty(metadata)) {
			String encodedMetadata = JacksonUtils.serialize2Json(metadata);
			try {
				builder.header(headerName, URLEncoder.encode(encodedMetadata, UTF_8));
			}
			catch (UnsupportedEncodingException e) {
				builder.header(headerName, encodedMetadata);
			}
		}
	}
}
