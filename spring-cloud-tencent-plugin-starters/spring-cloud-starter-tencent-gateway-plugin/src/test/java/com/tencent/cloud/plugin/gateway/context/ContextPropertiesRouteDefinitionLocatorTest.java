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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.cloud.gateway.route.RouteDefinition;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ContextPropertiesRouteDefinitionLocator}.
 */
class ContextPropertiesRouteDefinitionLocatorTest {

	@Test
	void shouldReturnEmptyWhenNoRoutes() {

		ContextGatewayProperties mockProps = mock(ContextGatewayProperties.class);
		when(mockProps.getRoutes()).thenReturn(Collections.emptyMap());

		ContextPropertiesRouteDefinitionLocator locator = new ContextPropertiesRouteDefinitionLocator(mockProps);

		Flux<RouteDefinition> result = locator.getRouteDefinitions();
		StepVerifier.create(result)
				.expectNextCount(0)
				.verifyComplete();
	}

	@Test
	void shouldReturnAllRouteDefinitions() {

		Map<String, RouteDefinition> testRoutes = new HashMap<>();
		testRoutes.put("route1", new RouteDefinition());
		testRoutes.put("route2", new RouteDefinition());

		ContextGatewayProperties mockProps = mock(ContextGatewayProperties.class);
		when(mockProps.getRoutes()).thenReturn(testRoutes);

		ContextPropertiesRouteDefinitionLocator locator = new ContextPropertiesRouteDefinitionLocator(mockProps);

		Flux<RouteDefinition> result = locator.getRouteDefinitions();
		StepVerifier.create(result)
				.expectNextCount(2)
				.verifyComplete();
	}
}
