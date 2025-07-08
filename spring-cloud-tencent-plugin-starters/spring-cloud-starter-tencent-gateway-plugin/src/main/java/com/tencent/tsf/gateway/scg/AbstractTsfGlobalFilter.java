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

package com.tencent.tsf.gateway.scg;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;

/**
 * Compatible with old versions TSF SDK.
 *
 * @author Shedfree Wu
 */
public abstract class AbstractTsfGlobalFilter implements GlobalFilter, Ordered {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if (shouldFilter(exchange, chain)) {
			return doFilter(exchange, chain);
		}
		else {
			return chain.filter(exchange);
		}
	}

	@Override
	abstract public int getOrder();

	abstract public boolean shouldFilter(ServerWebExchange exchange, GatewayFilterChain chain);

	abstract public Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain);
}
