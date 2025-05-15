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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ContextGatewayPropertiesManager}.
 */
class ContextGatewayPropertiesManagerTest {

	private ContextGatewayPropertiesManager manager;
	private PolarisDiscoveryClient mockDiscoveryClient;
	private PolarisReactiveDiscoveryClient mockReactiveClient;

	@BeforeEach
	void setup() {
		manager = new ContextGatewayPropertiesManager();
		mockDiscoveryClient = Mockito.mock(PolarisDiscoveryClient.class);
		mockReactiveClient = Mockito.mock(PolarisReactiveDiscoveryClient.class);
	}

	@Test
	void shouldHandleEmptyGroupsWhenSettingRouteMap() {
		// Test empty groups handling
		manager.setGroupRouteMap(null);
		assertThat(manager.getGroupPathRouteMap()).isEmpty();
		assertThat(manager.getGroups()).isNull();
	}

	@Test
	void shouldClassifyRoutesByPathType() {
		// Prepare test data
		Map<String, GroupContext> groups = new HashMap<>();
		GroupContext group1 = new GroupContext();
		GroupContext.ContextRoute normalRoute = new GroupContext.ContextRoute();
		normalRoute.setPath("/api/v1/normal");
		normalRoute.setMethod("POST");
		normalRoute.setNamespace("testNS");
		normalRoute.setService("testSvc");
		GroupContext.ContextRoute wildcardRoute = new GroupContext.ContextRoute();
		wildcardRoute.setPath("/api/wildcard/**");
		wildcardRoute.setMethod("GET");
		wildcardRoute.setNamespace("testNS");
		wildcardRoute.setService("testSvc");
		group1.setRoutes(Arrays.asList(normalRoute, wildcardRoute));
		GroupContext.ContextPredicate predicate = new GroupContext.ContextPredicate();
		predicate.setApiType(ApiType.MS);
		group1.setPredicate(predicate);
		groups.put("group1", group1);

		// Execute
		manager.setGroupRouteMap(groups);

		// Verify classification
		Map<String, Map<String, GroupContext.ContextRoute>> pathMap = manager.getGroupPathRouteMap();
		Map<String, Map<String, GroupContext.ContextRoute>> wildcardMap = manager.getGroupWildcardPathRouteMap();
		assertThat(pathMap.get("group1")).hasSize(1);
		assertThat(wildcardMap.get("group1")).hasSize(1);

		// Execute eager load
		manager.eagerLoad(mockDiscoveryClient, null);

		when(mockReactiveClient.getInstances("testSvc")).thenReturn(Flux.fromIterable(Collections.emptyList()));
		manager.eagerLoad(null, mockReactiveClient);

		manager.eagerLoad(null, null);
	}

	@Test
	void shouldMatchExactPathBeforeWildcard() {
		// Setup test routes
		GroupContext group = new GroupContext();
		GroupContext.ContextRoute exactRoute = new GroupContext.ContextRoute();
		exactRoute.setPath("/api/exact");
		exactRoute.setMethod("POST");
		exactRoute.setNamespace("testNS");
		exactRoute.setService("testSvc");
		GroupContext.ContextRoute wildcardRoute = new GroupContext.ContextRoute();
		wildcardRoute.setPath("/api/*/wildcard");
		wildcardRoute.setMethod("GET");
		wildcardRoute.setNamespace("testNS");
		wildcardRoute.setService("testSvc");

		group.setRoutes(Arrays.asList(exactRoute, wildcardRoute));
		GroupContext.ContextPredicate predicate = new GroupContext.ContextPredicate();
		predicate.setApiType(ApiType.MS);
		GroupContext.ContextNamespace namespace = new GroupContext.ContextNamespace();
		namespace.setPosition(Position.PATH);
		predicate.setNamespace(namespace);
		GroupContext.ContextService service = new GroupContext.ContextService();
		service.setPosition(Position.PATH);
		predicate.setService(service);
		group.setPredicate(predicate);
		manager.setGroupRouteMap(Collections.singletonMap("testGroup", group));


		ContextGatewayFilter filter = new ContextGatewayFilter(manager, null);
		MockServerHttpRequest request = MockServerHttpRequest.post("http://localhost/context/testNS/testSvc/api/exact").build();
		String[] apis = filter.rebuildMsApi(request, group, request.getPath().value());

		// Test path matching
		GroupContext.ContextRoute result = manager.getGroupPathRoute("testGroup", apis[0]);
		assertThat(result).isEqualTo(exactRoute);
		// Test wildcard matching
		request = MockServerHttpRequest.get("http://localhost/context/testNS/testSvc/api/test/wildcard").build();
		apis = filter.rebuildMsApi(request, group, request.getPath().value());
		result = manager.getGroupPathRoute("testGroup", apis[0]);
		assertThat(result).isEqualTo(wildcardRoute);
		// Test non-matching
		request = MockServerHttpRequest.get("http://localhost/context/testNS/testSvc/api/wildcard").build();
		apis = filter.rebuildMsApi(request, group, request.getPath().value());
		result = manager.getGroupPathRoute("testGroup", apis[0]);
		assertThat(result).isNull();
	}

	// Helper method to create test context route
	private GroupContext.ContextRoute createContextRoute(String path, String method, String namespace, String service) {
		GroupContext.ContextRoute route = new GroupContext.ContextRoute();
		route.setPath(path);
		route.setMethod(method);
		route.setNamespace(namespace);
		route.setService(service);
		return route;
	}

	// Helper method to create group context with configurable positions
	private GroupContext createGroupContext(ApiType apiType, Position namespacePos, Position servicePos) {
		GroupContext group = new GroupContext();
		GroupContext.ContextPredicate predicate = new GroupContext.ContextPredicate();
		predicate.setApiType(apiType);

		GroupContext.ContextNamespace namespace = new GroupContext.ContextNamespace();
		namespace.setPosition(namespacePos);
		namespace.setKey("ns-key");
		predicate.setNamespace(namespace);

		GroupContext.ContextService service = new GroupContext.ContextService();
		service.setPosition(servicePos);
		service.setKey("svc-key");
		predicate.setService(service);

		group.setPredicate(predicate);
		return group;
	}

	@Test
	void shouldHandleMultiplePositionCombinations() {
		// ns position PATH
		testPositionCombination(ApiType.MS, Position.PATH, Position.PATH,
				"/context/nsFromPath/svcFromPath/api/test",
				"POST|/nsFromPath/svcFromPath/api/test",
				"/api/test");

		testPositionCombination(ApiType.MS, Position.PATH, Position.HEADER,
				"/context/nsFromPath/api/test",
				"POST|/nsFromPath/svcFromHeader/api/test",
				"/api/test");

		testPositionCombination(ApiType.MS, Position.PATH, Position.QUERY,
				"/context/nsFromPath/api/test?svc-key=querySVC",
				"POST|/nsFromPath/querySVC/api/test",
				"/api/test");
		// ns position QUERY
		testPositionCombination(ApiType.MS, Position.QUERY, Position.PATH,
				"/context/svcFromPath/api/test?ns-key=queryNS",
				"POST|/queryNS/svcFromPath/api/test",
				"/api/test");

		testPositionCombination(ApiType.MS, Position.QUERY, Position.QUERY,
				"/context/api/test?ns-key=queryNS&svc-key=querySVC",
				"POST|/queryNS/querySVC/api/test",
				"/api/test");

		testPositionCombination(ApiType.MS, Position.QUERY, Position.HEADER,
				"/context/api/test?ns-key=queryNS",
				"POST|/queryNS/svcFromHeader/api/test",
				"/api/test");
		// ns position HEADER
		testPositionCombination(ApiType.MS, Position.HEADER, Position.PATH,
				"/context/svcFromPath/api/test",
				"POST|/headerNS/svcFromPath/api/test",
				"/api/test");

		testPositionCombination(ApiType.MS, Position.HEADER, Position.QUERY,
				"/context/api/test?svc-key=querySVC",
				"POST|/headerNS/querySVC/api/test",
				"/api/test");

		testPositionCombination(ApiType.MS, Position.HEADER, Position.HEADER,
				"/context/api/test",
				"POST|/headerNS/svcFromHeader/api/test",
				"/api/test");


	}

	private void testPositionCombination(ApiType apiType, Position namespacePos, Position servicePos,
			String inputPath, String expectedMatchPath, String expectedRealPath) {
		// Setup group with specified positions
		GroupContext group = createGroupContext(apiType, namespacePos, servicePos);
		group.setRoutes(Collections.singletonList(
				createContextRoute(expectedMatchPath, "POST", "testNS", "testSvc")
		));
		manager.setGroupRouteMap(Collections.singletonMap("testGroup", group));

		// Build test request with appropriate parameters
		MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.post(inputPath);
		switch (namespacePos) {
		case HEADER:
			requestBuilder.header("ns-key", "headerNS");
			break;
		case QUERY:
			// Query param already in URL
			break;
		}
		switch (servicePos) {
		case HEADER:
			requestBuilder.header("svc-key", "svcFromHeader");
			break;
		case QUERY:
			// Query param already in URL
			break;
		}

		ContextGatewayFilter filter = new ContextGatewayFilter(manager, null);
		MockServerHttpRequest mockServerHttpRequest = requestBuilder.build();
		String[] apis = filter.rebuildMsApi(mockServerHttpRequest, group, mockServerHttpRequest.getPath().value());

		// Verify path reconstruction
		assertThat(apis[0]).isEqualTo(expectedMatchPath);
		assertThat(apis[1]).isEqualTo(expectedRealPath);
	}

	@Test
	void shouldHandleExternalApiType() {
		// Test EXTERNAL API type
		GroupContext group = createGroupContext(ApiType.EXTERNAL, Position.PATH, Position.PATH);
		group.setRoutes(Collections.singletonList(
				createContextRoute("POST|/external/api", "POST", null, null)
		));
		manager.setGroupRouteMap(Collections.singletonMap("externalGroup", group));

		ContextGatewayFilter filter = new ContextGatewayFilter(manager, null);
		String inputPath = "/context/external/api";
		String[] apis = filter.rebuildExternalApi(
				MockServerHttpRequest.post(inputPath).build(),
				inputPath
		);

		assertThat(apis[0]).isEqualTo("POST|/external/api");
		assertThat(apis[1]).isEqualTo("/external/api");
	}

	@Test
	void testGroupContext() {
		GroupContext group1 = new GroupContext();
		group1.setComment("testComment");
		Assertions.assertEquals("testComment", group1.getComment());

		GroupContext.ContextPredicate contextPredicate = new GroupContext.ContextPredicate();

		contextPredicate.setContext("testContext");
		Assertions.assertEquals("testContext", contextPredicate.getContext());

		GroupContext.ContextRoute contextRoute = new GroupContext.ContextRoute();
		contextRoute.setPathMapping("testPathMapping");
		Assertions.assertEquals("testPathMapping", contextRoute.getPathMapping());
		contextRoute.setHost("testHost");
		Assertions.assertEquals("testHost", contextRoute.getHost());
		contextRoute.setMetadata(Collections.singletonMap("testKey", "testValue"));
		Assertions.assertEquals(1, contextRoute.getMetadata().size());
	}
}
