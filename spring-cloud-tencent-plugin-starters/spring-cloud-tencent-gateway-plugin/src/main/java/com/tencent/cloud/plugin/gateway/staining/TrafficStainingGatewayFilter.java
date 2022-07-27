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

package com.tencent.cloud.plugin.gateway.staining;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER;

/**
 * Staining the request, and the stained labels will be passed to the link through transitive metadata.
 * @author lepdou 2022-07-06
 */
public class TrafficStainingGatewayFilter implements GlobalFilter, Ordered {

	private static final Logger LOGGER = LoggerFactory.getLogger(TrafficStainingGatewayFilter.class);

	private final List<TrafficStainer> trafficStainers;

	public TrafficStainingGatewayFilter(List<TrafficStainer> trafficStainers) {
		if (!CollectionUtils.isEmpty(trafficStainers)) {
			trafficStainers.sort(Comparator.comparingInt(Ordered::getOrder));
		}
		this.trafficStainers = trafficStainers;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if (CollectionUtils.isEmpty(trafficStainers)) {
			return chain.filter(exchange);
		}

		// 1. get stained labels from request
		Map<String, String> stainedLabels = getStainedLabels(exchange);

		if (CollectionUtils.isEmpty(stainedLabels)) {
			return chain.filter(exchange);
		}

		// 2. put stained labels to metadata context
		ServerHttpRequest request = exchange.getRequest().mutate().headers((httpHeaders) -> {
			MetadataContext metadataContext = exchange.getAttribute(MetadataConstant.HeaderName.METADATA_CONTEXT);
			if (metadataContext == null) {
				metadataContext = MetadataContextHolder.get();
			}

			Map<String, String> oldTransitiveMetadata = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

			// append new transitive metadata
			Map<String, String> newTransitiveMetadata = new HashMap<>(oldTransitiveMetadata);
			newTransitiveMetadata.putAll(stainedLabels);

			metadataContext.putFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE, newTransitiveMetadata);
		}).build();

		return chain.filter(exchange.mutate().request(request).build());
	}

	Map<String, String> getStainedLabels(ServerWebExchange exchange) {
		Map<String, String> stainedLabels = new HashMap<>();
		int size = trafficStainers.size();
		TrafficStainer stainer = null;
		for (int i = size - 1; i >= 0; i--) {
			try {
				stainer = trafficStainers.get(i);
				Map<String, String> labels = stainer.apply(exchange);
				if (!CollectionUtils.isEmpty(labels)) {
					stainedLabels.putAll(labels);
				}
			}
			catch (Exception e) {
				if (stainer != null) {
					LOGGER.error("[SCT] traffic stained error. stainer = {}", stainer.getClass().getName(), e);
				}
			}
		}
		LOGGER.debug("[SCT] traffic stained labels. {}", JacksonUtils.serialize2Json(stainedLabels));

		return stainedLabels;
	}

	@Override
	public int getOrder() {
		return ROUTE_TO_URL_FILTER_ORDER + 1;
	}
}
