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

package com.tencent.cloud.plugin.gateway.context;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.http.server.PathContainer;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_PATH_CONTAINER_ATTR;

public class ContextRoutePredicateFactory extends AbstractRoutePredicateFactory<ContextRoutePredicateFactory.Config> {

	private final ContextGatewayPropertiesManager manager;

	public ContextRoutePredicateFactory(ContextGatewayPropertiesManager manager) {
		super(Config.class);
		this.manager = manager;
	}

	@Override
	public Predicate<ServerWebExchange> apply(Config config) {
		return new GatewayPredicate() {
			@Override
			public boolean test(ServerWebExchange exchange) {
				if (exchange.getAttributes().containsKey(GATEWAY_PREDICATE_PATH_CONTAINER_ATTR)) {
					return true;
				}
				// do path-rewriting
				String path = exchange.getRequest().getURI().getRawPath();
				String rewritePath = PathRewriteUtil.getNewPath(manager.getPathRewrites(), path, exchange);
				exchange.getAttributes()
						.put(GATEWAY_PREDICATE_PATH_CONTAINER_ATTR, PathContainer.parsePath(rewritePath));
				return true;
			}

			@Override
			public Object getConfig() {
				return config;
			}

			@Override
			public String toString() {
				return String.format("Config: %s", config);
			}
		};
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return List.of("group");
	}

	public static class Config {
		private String group;

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
		}

		@Override
		public String toString() {
			return "Config{" +
					"group='" + group + '\'' +
					'}';
		}
	}
}
