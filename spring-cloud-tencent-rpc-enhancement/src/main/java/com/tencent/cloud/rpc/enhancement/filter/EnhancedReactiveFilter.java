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

package com.tencent.cloud.rpc.enhancement.filter;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * EnhancedReactiveFilter.
 *
 * @author sean yu
 */
public class EnhancedReactiveFilter implements WebFilter, Ordered {

	private final EnhancedPluginRunner pluginRunner;

	public EnhancedReactiveFilter(EnhancedPluginRunner pluginRunner) {
		this.pluginRunner = pluginRunner;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		EnhancedPluginContext enhancedPluginContext = new EnhancedPluginContext();

		EnhancedRequestContext enhancedRequestContext = EnhancedRequestContext.builder()
				.httpHeaders(exchange.getRequest().getHeaders())
				.httpMethod(exchange.getRequest().getMethod())
				.url(exchange.getRequest().getURI())
				.build();
		enhancedPluginContext.setRequest(enhancedRequestContext);

		enhancedPluginContext.setLocalServiceInstance(pluginRunner.getLocalServiceInstance());

		// Run pre enhanced plugins.
		pluginRunner.run(EnhancedPluginType.Server.PRE, enhancedPluginContext);

		long startMillis = System.currentTimeMillis();
		return chain.filter(exchange)
				.doOnSuccess(v -> {
					enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);

					EnhancedResponseContext enhancedResponseContext = EnhancedResponseContext.builder()
							.httpStatus(exchange.getResponse().getRawStatusCode())
							.httpHeaders(exchange.getResponse().getHeaders())
							.build();
					enhancedPluginContext.setResponse(enhancedResponseContext);

					// Run post enhanced plugins.
					pluginRunner.run(EnhancedPluginType.Server.POST, enhancedPluginContext);
				})
				.doOnError(e -> {
					enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);
					enhancedPluginContext.setThrowable(e);
					// Run exception enhanced plugins.
					pluginRunner.run(EnhancedPluginType.Server.EXCEPTION, enhancedPluginContext);
				})
				.doFinally(v -> {
					// Run finally enhanced plugins.
					pluginRunner.run(EnhancedPluginType.Server.FINALLY, enhancedPluginContext);
				});
	}

	@Override
	public int getOrder() {
		return OrderConstant.Server.Reactive.ENHANCED_FILTER_ORDER;
	}

}
