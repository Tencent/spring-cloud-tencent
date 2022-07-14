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

package com.tencent.cloud.polaris.router.feign;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.util.expresstion.ExpressionLabelUtils;
import feign.RequestTemplate;
import org.apache.commons.lang.StringUtils;

import org.springframework.util.CollectionUtils;

/**
 * Resolve rule expression label from feign request.
 *
 * @author lepdou 2022-05-20
 */
public final class FeignExpressionLabelUtils {

	private FeignExpressionLabelUtils() {
	}

	public static Map<String, String> resolve(RequestTemplate request, Set<String> labelKeys) {
		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		Map<String, String> labels = new HashMap<>();

		for (String labelKey : labelKeys) {
			if (StringUtils.startsWithIgnoreCase(labelKey, ExpressionLabelUtils.LABEL_HEADER_PREFIX)) {
				String headerKey = ExpressionLabelUtils.parseHeaderKey(labelKey);
				if (StringUtils.isBlank(headerKey)) {
					continue;
				}
				labels.put(labelKey, getHeaderValue(request, headerKey));
			}
			else if (StringUtils.startsWithIgnoreCase(labelKey, ExpressionLabelUtils.LABEL_QUERY_PREFIX)) {
				String queryKey = ExpressionLabelUtils.parseQueryKey(labelKey);
				if (StringUtils.isBlank(queryKey)) {
					continue;
				}
				labels.put(labelKey, getQueryValue(request, queryKey));
			}
			else if (StringUtils.equalsIgnoreCase(ExpressionLabelUtils.LABEL_METHOD, labelKey)) {
				labels.put(labelKey, request.method());
			}
			else if (StringUtils.equalsIgnoreCase(ExpressionLabelUtils.LABEL_URI, labelKey)) {
				URI uri = URI.create(request.request().url());
				labels.put(labelKey, uri.getPath());
			}
		}

		return labels;
	}

	public static String getHeaderValue(RequestTemplate request, String key) {
		Map<String, Collection<String>> headers = request.headers();
		return ExpressionLabelUtils.getFirstValue(headers, key);
	}

	public static String getQueryValue(RequestTemplate request, String key) {
		return ExpressionLabelUtils.getFirstValue(request.queries(), key);
	}
}
