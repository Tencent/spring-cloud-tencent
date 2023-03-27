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

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import reactor.core.publisher.Mono;

import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;

/**
 * web client filter used for writing metadata in HTTP request header.
 *
 * @author sean yu
 */
public class EncodeTransferMedataWebClientFilter implements ExchangeFilterFunction {

	@Override
	public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction next) {
		MetadataContext metadataContext = MetadataContextHolder.get();
		Map<String, String> customMetadata = metadataContext.getCustomMetadata();
		Map<String, String> disposableMetadata = metadataContext.getDisposableMetadata();
		Map<String, String> transHeaders = metadataContext.getTransHeadersKV();

		ClientRequest.Builder requestBuilder = ClientRequest.from(clientRequest);

		this.buildMetadataHeader(requestBuilder, customMetadata, CUSTOM_METADATA);
		this.buildMetadataHeader(requestBuilder, disposableMetadata, CUSTOM_DISPOSABLE_METADATA);
		this.buildTransmittedHeader(requestBuilder, transHeaders);

		ClientRequest request = requestBuilder.build();

		return next.exchange(request);
	}

	private void buildTransmittedHeader(ClientRequest.Builder requestBuilder, Map<String, String> transHeaders) {
		if (!CollectionUtils.isEmpty(transHeaders)) {
			transHeaders.forEach(requestBuilder::header);
		}
	}


	/**
	 * Set metadata into the request header for {@link ClientRequest} .
	 * @param requestBuilder instance of {@link ClientRequest.Builder}
	 * @param metadata metadata map .
	 * @param headerName target metadata http header name .
	 */
	private void buildMetadataHeader(ClientRequest.Builder requestBuilder, Map<String, String> metadata, String headerName) {
		if (!CollectionUtils.isEmpty(metadata)) {
			String encodedMetadata = JacksonUtils.serialize2Json(metadata);
			try {
				requestBuilder.header(headerName, URLEncoder.encode(encodedMetadata, UTF_8));
			}
			catch (UnsupportedEncodingException e) {
				requestBuilder.header(headerName, encodedMetadata);
			}
		}
	}

}
