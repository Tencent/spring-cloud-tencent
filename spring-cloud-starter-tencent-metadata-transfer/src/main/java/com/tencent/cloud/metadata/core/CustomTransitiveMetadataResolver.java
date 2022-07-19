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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.tencent.cloud.common.constant.MetadataConstant;
import org.apache.commons.lang.StringUtils;

import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolve custom transitive metadata from request.
 *
 * @author lepdou 2022-05-20
 */
public final class CustomTransitiveMetadataResolver {

	private CustomTransitiveMetadataResolver() {
	}

	public static Map<String, String> resolve(ServerWebExchange exchange) {
		Map<String, String> result = new HashMap<>();

		HttpHeaders headers = exchange.getRequest().getHeaders();
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String key = entry.getKey();
			if (StringUtils.isBlank(key)) {
				continue;
			}
			// resolve sct transitive header
			if (StringUtils.startsWithIgnoreCase(key, MetadataConstant.SCT_TRANSITIVE_HEADER_PREFIX)
					&& !CollectionUtils.isEmpty(entry.getValue())) {
				String sourceKey = StringUtils.substring(key, MetadataConstant.SCT_TRANSITIVE_HEADER_PREFIX_LENGTH);
				result.put(sourceKey, entry.getValue().get(0));
			}

			//resolve polaris transitive header
			if (StringUtils.startsWithIgnoreCase(key, MetadataConstant.POLARIS_TRANSITIVE_HEADER_PREFIX)
					&& !CollectionUtils.isEmpty(entry.getValue())) {
				String sourceKey = StringUtils.substring(key, MetadataConstant.POLARIS_TRANSITIVE_HEADER_PREFIX_LENGTH);
				result.put(sourceKey, entry.getValue().get(0));
			}
		}

		return result;
	}

	public static Map<String, String> resolve(HttpServletRequest request) {
		Map<String, String> result = new HashMap<>();

		Enumeration<String> headers = request.getHeaderNames();
		while (headers.hasMoreElements()) {
			String key = headers.nextElement();
			if (StringUtils.isBlank(key)) {
				continue;
			}

			// resolve sct transitive header
			if (StringUtils.startsWithIgnoreCase(key, MetadataConstant.SCT_TRANSITIVE_HEADER_PREFIX)
					&& StringUtils.isNotBlank(request.getHeader(key))) {
				String sourceKey = StringUtils.substring(key, MetadataConstant.SCT_TRANSITIVE_HEADER_PREFIX_LENGTH);
				result.put(sourceKey, request.getHeader(key));
			}

			// resolve polaris transitive header
			if (StringUtils.startsWithIgnoreCase(key, MetadataConstant.POLARIS_TRANSITIVE_HEADER_PREFIX)
					&& StringUtils.isNotBlank(request.getHeader(key))) {
				String sourceKey = StringUtils.substring(key, MetadataConstant.POLARIS_TRANSITIVE_HEADER_PREFIX_LENGTH);
				result.put(sourceKey, request.getHeader(key));
			}
		}

		return result;
	}
}
