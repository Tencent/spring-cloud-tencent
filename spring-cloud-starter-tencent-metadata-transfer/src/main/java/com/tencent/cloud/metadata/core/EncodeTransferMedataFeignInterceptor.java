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
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;
import static java.net.URLEncoder.encode;

/**
 * Interceptor used for adding the metadata in http headers from context when web client
 * is Feign.
 *
 * @author Haotian Zhang
 */
public class EncodeTransferMedataFeignInterceptor implements RequestInterceptor, Ordered {

	private static final Logger LOG = LoggerFactory.getLogger(EncodeTransferMedataFeignInterceptor.class);

	@Override
	public int getOrder() {
		return MetadataConstant.OrderConstant.METADATA_2_HEADER_INTERCEPTOR_ORDER;
	}

	@Override
	public void apply(RequestTemplate requestTemplate) {
		// get metadata of current thread
		MetadataContext metadataContext = MetadataContextHolder.get();
		Map<String, String> customMetadata = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		Map<String, String> disposableMetadata = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_DISPOSABLE);

		// Clean up one-time metadata coming from upstream .
		Map<String, String> newestCustomMetadata = new HashMap<>();
		customMetadata.forEach((key, value) -> {
			if (!disposableMetadata.containsKey(key)) {
				newestCustomMetadata.put(key, value);
			}
		});
		this.buildMetadataHeader(requestTemplate, disposableMetadata, CUSTOM_DISPOSABLE_METADATA);

		// process custom metadata finally
		this.buildMetadataHeader(requestTemplate, newestCustomMetadata, CUSTOM_METADATA);
	}

	/**
	 * Set metadata into the request header for {@link RestTemplate} .
	 * @param requestTemplate instance of {@link RestTemplate}
	 * @param metadata metadata map .
	 * @param headerName target metadata http header name .
	 */
	private void buildMetadataHeader(RequestTemplate requestTemplate, Map<String, String> metadata, String headerName) {
		if (!CollectionUtils.isEmpty(metadata)) {
			String encodedMetadata = JacksonUtils.serialize2Json(metadata);
			requestTemplate.removeHeader(headerName);
			try {
				requestTemplate.header(headerName, encode(encodedMetadata, UTF_8));
			}
			catch (UnsupportedEncodingException e) {
				LOG.error("Set header failed.", e);
				requestTemplate.header(headerName, encodedMetadata);
			}
		}
	}
}
