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

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.common.util.UrlUtils;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;

/**
 * Pre EnhancedPlugin for scg to encode transfer metadata.
 *
 * @author Shedfree Wu
 */
public class EncodeTransferMedataScgEnhancedPlugin implements EnhancedPlugin {
	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.PRE;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		if (!(context.getOriginRequest() instanceof ServerWebExchange)) {
			return;
		}
		ServerWebExchange exchange = (ServerWebExchange) context.getOriginRequest();

		// get request builder
		ServerHttpRequest.Builder builder = exchange.getRequest().mutate();

		// get metadata of current thread
		MetadataContext metadataContext = exchange.getAttribute(MetadataConstant.HeaderName.METADATA_CONTEXT);
		if (metadataContext == null) {
			metadataContext = MetadataContextHolder.get();
		}

		Map<String, String> customMetadata = metadataContext.getCustomMetadata();
		Map<String, String> disposableMetadata = metadataContext.getDisposableMetadata();

		MessageMetadataContainer calleeMessageMetadataContainer = metadataContext.getMetadataContainer(MetadataType.MESSAGE, false);
		Map<String, String> calleeTransitiveHeaders = calleeMessageMetadataContainer.getTransitiveHeaders();
		// currently only support transitive header from calleeMessageMetadataContainer
		this.buildHeaderMap(builder, calleeTransitiveHeaders);

		this.buildMetadataHeader(builder, customMetadata, CUSTOM_METADATA);
		this.buildMetadataHeader(builder, disposableMetadata, CUSTOM_DISPOSABLE_METADATA);
		TransHeadersTransfer.transfer(exchange.getRequest());

		context.setOriginRequest(exchange.mutate().request(builder.build()).build());
	}

	private void buildHeaderMap(ServerHttpRequest.Builder builder, Map<String, String> headerMap) {
		if (!CollectionUtils.isEmpty(headerMap)) {
			headerMap.forEach((key, value) -> builder.header(key, UrlUtils.encode(value)));
		}
	}

	/**
	 * Set metadata into the request header for {@link ServerHttpRequest.Builder} .
	 * @param builder instance of {@link ServerHttpRequest.Builder}
	 * @param metadata metadata map .
	 * @param headerName target metadata http header name .
	 */
	private void buildMetadataHeader(ServerHttpRequest.Builder builder, Map<String, String> metadata, String headerName) {
		if (!CollectionUtils.isEmpty(metadata)) {
			buildHeaderMap(builder, ImmutableMap.of(headerName, JacksonUtils.serialize2Json(metadata)));
		}
	}

	@Override
	public int getOrder() {
		return PluginOrderConstant.ClientPluginOrder.CONSUMER_TRANSFER_METADATA_PLUGIN_ORDER;
	}
}
