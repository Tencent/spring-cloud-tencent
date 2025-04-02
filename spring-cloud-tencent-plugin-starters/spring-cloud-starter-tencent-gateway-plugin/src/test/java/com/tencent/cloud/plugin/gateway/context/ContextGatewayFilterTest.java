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

import java.net.URI;
import java.util.Collections;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * Test class for {@link ContextGatewayFilter}.
 */
class ContextGatewayFilterTest {

	@Mock
	private ContextGatewayPropertiesManager mockManager;
	@Mock
	private GatewayFilterChain mockChain;

	private ContextGatewayFilter filter;
	private GroupContext groupContext;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		ContextGatewayFilterFactory.Config config = new ContextGatewayFilterFactory.Config();
		config.setGroup("testGroup");
		filter = new ContextGatewayFilter(mockManager, config);
		groupContext = new GroupContext();
		when(mockManager.getGroups()).thenReturn(Collections.singletonMap("testGroup", groupContext));
	}

	// Test EXTERNAL API path reconstruction
	@Test
	void shouldHandleExternalApiPathCorrectly() {
		// Setup group context
		GroupContext.ContextPredicate predicate = createPredicate(ApiType.EXTERNAL, Position.PATH, Position.PATH);
		groupContext.setPredicate(predicate);
		groupContext.setRoutes(Collections.singletonList(
				createContextRoute("GET|/external/api", "GET", "testNS", "testSvc")
		));

		// Create test request
		MockServerHttpRequest request = MockServerHttpRequest.get("/context/external/api").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		GroupContext.ContextRoute route = createContextRoute("GET|/external/api", "GET", "http://test.com");

		when(mockManager.getGroupPathRoute("testGroup", "GET|/external/api")).thenReturn(route);

		// Execute filter
		Mono<Void> result = filter.filter(exchange, mockChain);

		// Verify final URL
		URI finalUri = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
		assertThat(finalUri.toString()).isEqualTo("http://test.com/external/api");
	}

	// Test metadata update functionality
	@Test
	void shouldUpdateRouteMetadataCorrectly() throws Exception {
		// Setup context route with metadata
		GroupContext.ContextRoute route = createContextRoute("GET|/api", "GET", "ns", "svc");
		route.setMetadata(Collections.singletonMap("version", "v2"));

		// Setup group context
		GroupContext.ContextPredicate predicate = createPredicate(ApiType.MS, Position.PATH, Position.PATH);
		groupContext.setPredicate(predicate);
		groupContext.setRoutes(Collections.singletonList(route));

		// Create test request
		MockServerHttpRequest request = MockServerHttpRequest.get("/context/ns/svc/api").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, Route.async().id("test").uri(URI.create("lb://svc")).
				predicate((GatewayPredicate) serverWebExchange -> false).build());
		when(mockManager.getGroupPathRoute("testGroup", "GET|/ns/svc/api")).thenReturn(route);

		exchange.getAttributes().put(MetadataConstant.HeaderName.METADATA_CONTEXT, MetadataContextHolder.get());
		// Execute filter
		filter.filter(exchange, mockChain);

//		Route updatedRoute = exchange.getRequiredAttribute(GATEWAY_ROUTE_ATTR);
//		assertThat(updatedRoute.getMetadata()).containsEntry("version", "v2");
		// no context in exchange
		exchange.getAttributes().remove(MetadataConstant.HeaderName.METADATA_CONTEXT);
		filter.filter(exchange, mockChain);

	}

	// Helper method to create test predicate
	private GroupContext.ContextPredicate createPredicate(ApiType apiType, Position nsPos, Position svcPos) {
		GroupContext.ContextPredicate predicate = new GroupContext.ContextPredicate();
		predicate.setApiType(apiType);

		GroupContext.ContextNamespace namespace = new GroupContext.ContextNamespace();
		namespace.setPosition(nsPos);
		namespace.setKey("ns-key");
		predicate.setNamespace(namespace);

		GroupContext.ContextService service = new GroupContext.ContextService();
		service.setPosition(svcPos);
		service.setKey("svc-key");
		predicate.setService(service);

		return predicate;
	}

	// Helper method to create context route
	private GroupContext.ContextRoute createContextRoute(String path, String method, String ns, String svc) {
		GroupContext.ContextRoute route = new GroupContext.ContextRoute();
		route.setPath(path);
		route.setMethod(method);
		route.setNamespace(ns);
		route.setService(svc);
		return route;
	}

	private GroupContext.ContextRoute createContextRoute(String path, String method, String host) {
		GroupContext.ContextRoute route = new GroupContext.ContextRoute();
		route.setPath(path);
		route.setMethod(method);
		route.setHost(host);
		return route;
	}
}
