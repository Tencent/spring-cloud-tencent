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

package com.tencent.cloud.polaris.auth.filter;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.polaris.auth.utils.AuthenticateUtils;
import com.tencent.polaris.api.plugin.auth.AuthResult;
import com.tencent.polaris.auth.api.core.AuthAPI;
import com.tencent.polaris.auth.api.rpc.AuthResponse;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import static org.springframework.core.io.buffer.DefaultDataBufferFactory.DEFAULT_INITIAL_CAPACITY;

/**
 * Reactive filter to authenticate.
 *
 * @author Haotian Zhang
 */
public class AuthReactiveFilter implements WebFilter, Ordered {

	private final AuthAPI authAPI;

	public AuthReactiveFilter(AuthAPI authAPI) {
		this.authAPI = authAPI;
	}

	@Override
	public int getOrder() {
		return OrderConstant.Server.Reactive.AUTH_FILTER_ORDER;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		AuthResponse authResponse = AuthenticateUtils.authenticate(authAPI, MetadataContext.LOCAL_NAMESPACE,
				MetadataContext.LOCAL_SERVICE, exchange.getRequest().getURI().getPath(), "HTTP",
				exchange.getRequest().getMethod().name());
		if (authResponse != null && authResponse.getAuthResult().getCode()
				.equals(AuthResult.Code.AuthResultForbidden)) {
			ServerHttpResponse response = exchange.getResponse();
			response.setRawStatusCode(HttpStatus.FORBIDDEN.value());
			DataBuffer dataBuffer = response.bufferFactory().allocateBuffer(DEFAULT_INITIAL_CAPACITY);
			return response.writeWith(Mono.just(dataBuffer));
		}
		return chain.filter(exchange);
	}
}
