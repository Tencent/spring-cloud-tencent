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

import java.net.URI;
import java.util.Collection;
import java.util.Map;

import com.tencent.cloud.common.util.UrlUtils;
import com.tencent.cloud.common.util.expresstion.ExpressionLabelUtils;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataProvider;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import com.tencent.polaris.metadata.core.manager.CalleeMetadataContainerGroup;
import feign.RequestTemplate;

/**
 * MetadataProvider used for Feign RequestTemplate.
 *
 * @author Haotian Zhang
 */
public class FeignRequestTemplateMetadataProvider implements MetadataProvider {

	private final RequestTemplate requestTemplate;

	public FeignRequestTemplateMetadataProvider(RequestTemplate requestTemplate) {
		this.requestTemplate = requestTemplate;
	}

	@Override
	public String getRawMetadataStringValue(String key) {
		switch (key) {
		case MessageMetadataContainer.LABEL_KEY_METHOD:
			return requestTemplate.method();
		case MessageMetadataContainer.LABEL_KEY_PATH:
			URI uri = URI.create(requestTemplate.request().url());
			return UrlUtils.decode(uri.getPath());
		case MessageMetadataContainer.LABEL_KEY_CALLER_IP:
			return CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer()
					.getRawMetadataStringValue(MetadataConstants.LOCAL_IP);
		default:
			return null;
		}
	}

	@Override
	public String getRawMetadataMapValue(String key, String mapKey) {
		Map<String, Collection<String>> headers = requestTemplate.headers();
		switch (key) {
		case MessageMetadataContainer.LABEL_MAP_KEY_HEADER:
			return UrlUtils.decode(ExpressionLabelUtils.getFirstValue(headers, mapKey));
		case MessageMetadataContainer.LABEL_MAP_KEY_COOKIE:
			return UrlUtils.decode(ExpressionLabelUtils.getCookieFirstValue(headers, mapKey));
		case MessageMetadataContainer.LABEL_MAP_KEY_QUERY:
			return UrlUtils.decode(ExpressionLabelUtils.getFirstValue(requestTemplate.queries(), mapKey));
		default:
			return null;
		}
	}
}
