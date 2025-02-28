/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.plugin.gateway;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.web.server.ServerWebExchange;

public class PolarisReactiveLoadBalancerClientFilter extends ReactiveLoadBalancerClientFilter {

	private final ReactiveLoadBalancerClientFilter clientFilter;

	public PolarisReactiveLoadBalancerClientFilter(LoadBalancerClientFactory clientFactory,
			GatewayLoadBalancerProperties properties, ReactiveLoadBalancerClientFilter clientFilter)  {
		super(clientFactory, properties);
		this.clientFilter = clientFilter;
	}

	@Override
	public int getOrder() {
		return clientFilter.getOrder();
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// restore context from exchange
		MetadataContext metadataContext = exchange.getAttribute(
				MetadataConstant.HeaderName.METADATA_CONTEXT);
		if (metadataContext != null) {
			MetadataContextHolder.set(metadataContext);
		}
		return clientFilter.filter(exchange, chain);
	}
}
