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

package com.tencent.cloud.plugin.gateway.context;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextRoutePredicateFactory}.
 */
class ContextRoutePredicateFactoryTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(ContextRoutePredicateFactory.class);

	@Test
	void shouldCreateConfigWithGroup() {
		contextRunner.run(context -> {
			// Arrange
			ContextRoutePredicateFactory factory = context.getBean(ContextRoutePredicateFactory.class);
			factory.shortcutFieldOrder();

			// Act
			ContextRoutePredicateFactory.Config config = factory.newConfig();
			config.setGroup("g1");
			Assertions.assertEquals("g1", config.getGroup());

			GatewayPredicate gatewayPredicate = (GatewayPredicate) factory.apply(config);
			gatewayPredicate.toString();
			Assertions.assertTrue(gatewayPredicate.test(null));
			Assertions.assertEquals(config, gatewayPredicate.getConfig());
		});
	}

	@Test
	void shouldAlwaysMatchWhenNotImplemented() {
		contextRunner.run(context -> {
			// Arrange
			ContextRoutePredicateFactory factory = context.getBean(ContextRoutePredicateFactory.class);
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/test").build());

			// Act
			ContextRoutePredicateFactory.Config config = new ContextRoutePredicateFactory.Config();
			config.setGroup("test-group");

			// Assert
			boolean result = factory.apply(config).test(exchange);
			assertThat(result).isTrue();
		});
	}

	@Test
	void shouldSupportShortcutFieldOrder() {
		contextRunner.run(context -> {
			ContextRoutePredicateFactory factory = context.getBean(ContextRoutePredicateFactory.class);
			assertThat(factory.shortcutFieldOrder()).containsExactly("group");
		});
	}

	@Test
	void shouldCreateValidRoutePredicate() {
		contextRunner.run(context -> {
			// Arrange
			Route route = Route.async()
					.id("test-route")
					.uri("http://example.com")
					.predicate(context.getBean(ContextRoutePredicateFactory.class)
							.apply(c -> c.setGroup("group1")))
					.build();

			// Act
			assertThat(route.getPredicate()).isNotNull();
		});
	}
}
