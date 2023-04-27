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

package com.tencent.cloud.polaris.circuitbreaker.zuul;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;

/**
 * Zuul fallback factory.
 *
 * @author Haotian Zhang
 */
public class PolarisZuulFallbackFactory {
	private final Map<String, FallbackProvider> fallbackProviderCache;

	private FallbackProvider defaultFallbackProvider = null;

	public PolarisZuulFallbackFactory(Set<FallbackProvider> fallbackProviders) {
		this.fallbackProviderCache = new HashMap<>();
		for (FallbackProvider provider : fallbackProviders) {
			String route = provider.getRoute();
			if ("*".equals(route) || route == null) {
				defaultFallbackProvider = provider;
			}
			else {
				fallbackProviderCache.put(route, provider);
			}
		}
	}

	public FallbackProvider getFallbackProvider(String route) {
		FallbackProvider provider = fallbackProviderCache.get(route);
		if (provider == null) {
			provider = defaultFallbackProvider;
		}
		return provider;
	}

}
