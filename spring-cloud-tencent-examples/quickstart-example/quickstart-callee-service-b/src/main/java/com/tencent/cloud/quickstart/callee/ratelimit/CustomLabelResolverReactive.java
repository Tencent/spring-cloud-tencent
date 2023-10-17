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
 */

package com.tencent.cloud.quickstart.callee.ratelimit;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelReactiveResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * resolver custom label from request.
 *
 * @author sean yu
 */
@Component
public class CustomLabelResolverReactive implements PolarisRateLimiterLabelReactiveResolver {
	private static final Logger LOG = LoggerFactory.getLogger(CustomLabelResolverReactive.class);

	@Value("${label.key-value:}")
	private String[] keyValues;

	@Override
	public Map<String, String> resolve(ServerWebExchange exchange) {
		// rate limit by some request params. such as query params, headers ..
		return getLabels(keyValues);
	}

	private Map<String, String> getLabels(String[] keyValues) {
		Map<String, String> labels = new HashMap<>();
		for (String kv : keyValues) {
			String key = kv.substring(0, kv.indexOf(":"));
			String value = kv.substring(kv.indexOf(":") + 1);
			labels.put(key, value);
		}

		LOG.info("Current labels:{}", labels);
		return labels;
	}
}
