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

package com.tencent.cloud.metadata.provider;

import com.tencent.cloud.common.util.UrlUtils;
import com.tencent.cloud.common.util.expresstion.SpringWebExpressionLabelUtils;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataProvider;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import com.tencent.polaris.metadata.core.manager.CalleeMetadataContainerGroup;

import org.springframework.http.HttpRequest;

/**
 * MetadataProvider used for RestTemplate HttpRequest.
 *
 * @author Haotian Zhang
 */
public class RestTemplateMetadataProvider implements MetadataProvider {

	private final HttpRequest request;

	public RestTemplateMetadataProvider(HttpRequest request) {
		this.request = request;
	}

	@Override
	public String doGetRawMetadataStringValue(String key) {
		switch (key) {
		case MessageMetadataContainer.LABEL_KEY_METHOD:
			return request.getMethod().toString();
		case MessageMetadataContainer.LABEL_KEY_PATH:
			return UrlUtils.decode(request.getURI().getPath());
		case MessageMetadataContainer.LABEL_KEY_CALLER_IP:
			return CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer()
					.getRawMetadataStringValue(MetadataConstants.LOCAL_IP);
		default:
			return null;
		}
	}

	@Override
	public String doGetRawMetadataMapValue(String key, String mapKey) {
		switch (key) {
		case MessageMetadataContainer.LABEL_MAP_KEY_HEADER:
			return UrlUtils.decode(SpringWebExpressionLabelUtils.getHeaderValue(request, mapKey));
		case MessageMetadataContainer.LABEL_MAP_KEY_COOKIE:
			return UrlUtils.decode(SpringWebExpressionLabelUtils.getCookieValue(request, mapKey));
		case MessageMetadataContainer.LABEL_MAP_KEY_QUERY:
			return UrlUtils.decode(SpringWebExpressionLabelUtils.getQueryValue(request, mapKey));
		default:
			return null;
		}
	}
}
