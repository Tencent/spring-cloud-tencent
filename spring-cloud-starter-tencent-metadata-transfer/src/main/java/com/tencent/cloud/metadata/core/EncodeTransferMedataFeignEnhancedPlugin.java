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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.common.util.ReflectionUtils;
import com.tencent.cloud.common.util.UrlUtils;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import feign.Request;

import org.springframework.util.CollectionUtils;

import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;

/**
 * Pre EnhancedPlugin for feign to encode transfer metadata.
 *
 * @author Shedfree Wu
 */
public class EncodeTransferMedataFeignEnhancedPlugin implements EnhancedPlugin {
	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.PRE;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		if (!(context.getOriginRequest() instanceof Request)) {
			return;
		}
		Request request = (Request) context.getOriginRequest();

		// get metadata of current thread
		MetadataContext metadataContext = MetadataContextHolder.get();
		Map<String, String> customMetadata = metadataContext.getCustomMetadata();
		Map<String, String> disposableMetadata = metadataContext.getDisposableMetadata();
		Map<String, String> transHeaders = metadataContext.getTransHeadersKV();

		MessageMetadataContainer calleeMessageMetadataContainer = metadataContext.getMetadataContainer(MetadataType.MESSAGE, false);
		Map<String, String> calleeTransitiveHeaders = calleeMessageMetadataContainer.getTransitiveHeaders();
		// currently only support transitive header from calleeMessageMetadataContainer
		this.buildHeaderMap(request, calleeTransitiveHeaders);

		// build custom disposable metadata request header
		this.buildMetadataHeader(request, disposableMetadata, CUSTOM_DISPOSABLE_METADATA);

		// process custom metadata
		this.buildMetadataHeader(request, customMetadata, CUSTOM_METADATA);

		// set headers that need to be transmitted from the upstream
		this.buildTransmittedHeader(request, transHeaders);
	}

	private void buildTransmittedHeader(Request request, Map<String, String> transHeaders) {
		if (!CollectionUtils.isEmpty(transHeaders)) {
			Map<String, Collection<String>> headers = getModifiableHeaders(request);
			transHeaders.entrySet().stream().forEach(entry -> {
				headers.remove(entry.getKey());
				headers.put(entry.getKey(), Arrays.asList(entry.getValue()));
			});
		}
	}

	/**
	 * Set metadata into the request header for {@link Request} .
	 * @param request instance of {@link Request}
	 * @param metadata metadata map .
	 * @param headerName target metadata http header name .
	 */
	private void buildMetadataHeader(Request request, Map<String, String> metadata, String headerName) {
		if (!CollectionUtils.isEmpty(metadata)) {
			buildHeaderMap(request, ImmutableMap.of(headerName, JacksonUtils.serialize2Json(metadata)));
		}
	}


	/**
	 * Set headerMap into the request header for {@link Request} .
	 * @param request instance of {@link Request}
	 * @param headerMap header map .
	 */
	private void buildHeaderMap(Request request, Map<String, String> headerMap) {
		if (!CollectionUtils.isEmpty(headerMap)) {
			Map<String, Collection<String>> headers = getModifiableHeaders(request);
			headerMap.forEach((key, value) -> headers.put(key, Arrays.asList(UrlUtils.encode(value))));
		}
	}

	/**
	 * The value obtained directly from the headers method is an unmodifiable map.
	 * If the Feign client uses the URL, the original headers are unmodifiable.
	 * @param request feign request
	 * @return modifiable headers
	 */
	private Map<String, Collection<String>> getModifiableHeaders(Request request) {
		Map<String, Collection<String>> headers;
		headers = (Map<String, Collection<String>>) ReflectionUtils.getFieldValue(request, "headers");

		if (!(headers instanceof LinkedHashMap)) {
			headers = new LinkedHashMap<>(headers);
			ReflectionUtils.setFieldValue(request, "headers", headers);
		}
		return headers;
	}

	@Override
	public int getOrder() {
		return PluginOrderConstant.ClientPluginOrder.CONSUMER_TRANSFER_METADATA_PLUGIN_ORDER;
	}
}
