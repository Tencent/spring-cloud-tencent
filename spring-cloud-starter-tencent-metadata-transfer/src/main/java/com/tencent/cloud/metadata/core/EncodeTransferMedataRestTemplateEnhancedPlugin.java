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

import org.springframework.http.HttpRequest;
import org.springframework.util.CollectionUtils;

import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;

/**
 * Pre EnhancedPlugin for rest template to encode transfer metadata.
 *
 * @author Shedfree Wu
 */
public class EncodeTransferMedataRestTemplateEnhancedPlugin implements EnhancedPlugin {
	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.PRE;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		if (!(context.getOriginRequest() instanceof HttpRequest)) {
			return;
		}
		HttpRequest httpRequest = (HttpRequest) context.getOriginRequest();

		// get metadata of current thread
		MetadataContext metadataContext = MetadataContextHolder.get();
		Map<String, String> customMetadata = metadataContext.getCustomMetadata();
		Map<String, String> disposableMetadata = metadataContext.getDisposableMetadata();
		Map<String, String> transHeaders = metadataContext.getTransHeadersKV();
		MessageMetadataContainer calleeMessageMetadataContainer = metadataContext.getMetadataContainer(MetadataType.MESSAGE, false);
		Map<String, String> calleeTransitiveHeaders = calleeMessageMetadataContainer.getTransitiveHeaders();
		// currently only support transitive header from calleeMessageMetadataContainer
		this.buildHeaderMap(httpRequest, calleeTransitiveHeaders);

		// build custom disposable metadata request header
		this.buildMetadataHeader(httpRequest, disposableMetadata, CUSTOM_DISPOSABLE_METADATA);

		// build custom metadata request header
		this.buildMetadataHeader(httpRequest, customMetadata, CUSTOM_METADATA);

		// set headers that need to be transmitted from the upstream
		this.buildTransmittedHeader(httpRequest, transHeaders);
	}

	private void buildTransmittedHeader(HttpRequest request, Map<String, String> transHeaders) {
		if (!CollectionUtils.isEmpty(transHeaders)) {
			transHeaders.entrySet().stream().forEach(entry -> {
				request.getHeaders().set(entry.getKey(), entry.getValue());
			});
		}
	}

	private void buildHeaderMap(HttpRequest request, Map<String, String> headerMap) {
		if (!CollectionUtils.isEmpty(headerMap)) {
			headerMap.forEach((key, value) -> request.getHeaders().set(key, UrlUtils.encode(value)));
		}
	}

	/**
	 * Set metadata into the request header for {@link HttpRequest} .
	 *
	 * @param request    instance of {@link HttpRequest}
	 * @param metadata   metadata map .
	 * @param headerName target metadata http header name .
	 */
	private void buildMetadataHeader(HttpRequest request, Map<String, String> metadata, String headerName) {
		if (!CollectionUtils.isEmpty(metadata)) {
			buildHeaderMap(request, ImmutableMap.of(headerName, JacksonUtils.serialize2Json(metadata)));
		}
	}

	@Override
	public int getOrder() {
		return PluginOrderConstant.ClientPluginOrder.CONSUMER_TRANSFER_METADATA_PLUGIN_ORDER;
	}
}
