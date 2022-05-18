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

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;

import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Interceptor used for adding the metadata in http headers from context when web client
 * is RestTemplate.
 *
 * @author Haotian Zhang
 */
public class EncodeTransferMedataRestTemplateInterceptor
		implements ClientHttpRequestInterceptor, Ordered {

	@Override
	public int getOrder() {
		return MetadataConstant.OrderConstant.METADATA_2_HEADER_INTERCEPTOR_ORDER;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
			ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
		// get metadata of current thread
		MetadataContext metadataContext = MetadataContextHolder.get();

		// add new metadata and cover old
		String metadataStr = httpRequest.getHeaders()
				.getFirst(MetadataConstant.HeaderName.CUSTOM_METADATA);
		if (!StringUtils.isEmpty(metadataStr)) {
			Map<String, String> headerMetadataMap = JacksonUtils
					.deserialize2Map(metadataStr);
			for (String key : headerMetadataMap.keySet()) {
				metadataContext.putContext(MetadataContext.FRAGMENT_TRANSITIVE, key, headerMetadataMap.get(key));
			}
		}
		Map<String, String> customMetadata = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		if (!CollectionUtils.isEmpty(customMetadata)) {
			metadataStr = JacksonUtils.serialize2Json(customMetadata);
			try {
				httpRequest.getHeaders().set(MetadataConstant.HeaderName.CUSTOM_METADATA,
						URLEncoder.encode(metadataStr, "UTF-8"));
			}
			catch (UnsupportedEncodingException e) {
				httpRequest.getHeaders().set(MetadataConstant.HeaderName.CUSTOM_METADATA,
						metadataStr);
			}
		}
		return clientHttpRequestExecution.execute(httpRequest, bytes);
	}

}
