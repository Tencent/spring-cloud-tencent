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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;

import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;

/**
 * Interceptor used for adding the metadata in http headers from context when web client
 * is RestTemplate.
 *
 * @author Haotian Zhang
 */
public class EncodeTransferMedataRestTemplateInterceptor implements ClientHttpRequestInterceptor, Ordered {

	@Override
	public int getOrder() {
		return OrderConstant.Client.RestTemplate.ENCODE_TRANSFER_METADATA_INTERCEPTOR_ORDER;
	}

	@Override
	public ClientHttpResponse intercept(@NonNull HttpRequest httpRequest, @NonNull byte[] bytes,
			@NonNull ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
		// get metadata of current thread
		MetadataContext metadataContext = MetadataContextHolder.get();
		Map<String, String> customMetadata = metadataContext.getCustomMetadata();
		Map<String, String> disposableMetadata = metadataContext.getDisposableMetadata();
		Map<String, String> transHeaders = metadataContext.getTransHeadersKV();

		// build custom disposable metadata request header
		this.buildMetadataHeader(httpRequest, disposableMetadata, CUSTOM_DISPOSABLE_METADATA);

		// build custom metadata request header
		this.buildMetadataHeader(httpRequest, customMetadata, CUSTOM_METADATA);

		// set headers that need to be transmitted from the upstream
		this.buildTransmittedHeader(httpRequest, transHeaders);

		return clientHttpRequestExecution.execute(httpRequest, bytes);
	}

	private void buildTransmittedHeader(HttpRequest request, Map<String, String> transHeaders) {
		if (!CollectionUtils.isEmpty(transHeaders)) {
			transHeaders.entrySet().stream().forEach(entry -> {
				request.getHeaders().set(entry.getKey(), entry.getValue());
			});
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
			String encodedMetadata = JacksonUtils.serialize2Json(metadata);
			try {
				request.getHeaders().set(headerName, URLEncoder.encode(encodedMetadata, UTF_8));
			}
			catch (UnsupportedEncodingException e) {
				request.getHeaders().set(headerName, encodedMetadata);
			}
		}
	}
}
