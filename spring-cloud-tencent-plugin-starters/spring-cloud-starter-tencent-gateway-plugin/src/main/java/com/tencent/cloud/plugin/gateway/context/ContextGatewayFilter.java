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

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.HashMap;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.polaris.api.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

public class ContextGatewayFilter implements GatewayFilter, Ordered {

	private static final Logger logger = LoggerFactory.getLogger(ContextGatewayFilter.class);

	private ContextGatewayPropertiesManager manager;

	private ContextGatewayFilterFactory.Config config;

	public ContextGatewayFilter(ContextGatewayPropertiesManager manager, ContextGatewayFilterFactory.Config config) {
		this.manager = manager;
		this.config = config;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		GroupContext groupContext = manager.getGroups().get(config.getGroup());

		if (ApiType.MS.equals(groupContext.getPredicate().getApiType())) {
			return msFilter(exchange, chain, groupContext);
		}
		else {
			return externalFilter(exchange, chain, groupContext);
		}
	}

	private Mono<Void> externalFilter(ServerWebExchange exchange, GatewayFilterChain chain, GroupContext groupContext) {
		ServerHttpRequest request = exchange.getRequest();
		String[] apis = rebuildExternalApi(request, request.getPath().value());
		GroupContext.ContextRoute contextRoute = manager.getGroupPathRoute(config.getGroup(), apis[0]);
		if (contextRoute == null) {
			throw new RuntimeException(String.format("Can't find context route for group: %s, path: %s, origin path: %s", config.getGroup(), apis[0], request.getPath()));
		}
		updateRouteMetadata(exchange, contextRoute);

		URI requestUri = URI.create(contextRoute.getHost() + apis[1]);
		exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUri);
		// 调整为正确路径
		ServerHttpRequest newRequest = request.mutate().path(apis[1]).build();
		return chain.filter(exchange.mutate().request(newRequest).build());
	}

	private Mono<Void> msFilter(ServerWebExchange exchange, GatewayFilterChain chain, GroupContext groupContext) {
		ServerHttpRequest request = exchange.getRequest();
		String[] apis = rebuildMsApi(request, groupContext, request.getPath().value());
		// 判断 api 是否匹配
		GroupContext.ContextRoute contextRoute = manager.getGroupPathRoute(config.getGroup(), apis[0]);
		if (contextRoute == null) {
			throw new RuntimeException(String.format("Can't find context route for group: %s, path: %s, origin path: %s", config.getGroup(), apis[0], request.getPath()));
		}
		updateRouteMetadata(exchange, contextRoute);

		MetadataContext metadataContext = (MetadataContext) exchange.getAttributes().get(
				MetadataConstant.HeaderName.METADATA_CONTEXT);

		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
				MetadataConstant.POLARIS_TARGET_NAMESPACE, contextRoute.getNamespace());

		URI requestUri = URI.create("lb://" + contextRoute.getService() + apis[1]);
		exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUri);
		// 调整为正确路径
		ServerHttpRequest newRequest = request.mutate().path(apis[1]).build();
		return chain.filter(exchange.mutate().request(newRequest).build());
	}

	/**
	 * e.g. "/context/api/test" → [ "GET|/api/test", "/api/test"]
	 */
	private String[] rebuildExternalApi(ServerHttpRequest request, String path) {
		String[] pathSegments = path.split("/");
		StringBuilder matchPath = new StringBuilder();
		StringBuilder realPath = new StringBuilder();
		int index = 2;
		matchPath.append(request.getMethod().name()).append("|");
		for (int i = index; i < pathSegments.length; i++) {
			matchPath.append("/").append(pathSegments[i]);
			realPath.append("/").append(pathSegments[i]);
		}
		if (path.endsWith("/")) {
			matchPath.append("/");
			realPath.append("/");
		}
		return new String[] {matchPath.toString(), realPath.toString()};
	}

	/**
	 * returns an array of two strings, the first is the match path, the second is the real path.
	 * e.g. "/context/namespace/svc/api/test" → [ "GET|/namespace/svc/api/test", "/api/test"]
	 */
	private String[] rebuildMsApi(ServerHttpRequest request, GroupContext groupContext, String path) {
		String[] pathSegments = path.split("/");
		StringBuilder matchPath = new StringBuilder();
		int index = 2;
		matchPath.append(request.getMethod().name()).append("|");

		Position namespacePosition = groupContext.getPredicate().getNamespace().getPosition();
		switch (namespacePosition) {
		case QUERY:
			matchPath.append("/")
					.append(request.getQueryParams().getFirst(groupContext.getPredicate().getNamespace().getKey()));
			break;
		case HEADER:
			matchPath.append("/")
					.append(request.getHeaders().getFirst(groupContext.getPredicate().getNamespace().getKey()));
			break;
		case PATH:
		default:
			matchPath.append("/").append(pathSegments[index++]);
			break;
		}
		Position servicePosition = groupContext.getPredicate().getService().getPosition();
		switch (servicePosition) {
		case QUERY:
			matchPath.append("/")
					.append(request.getQueryParams().getFirst(groupContext.getPredicate().getService().getKey()));
			break;
		case HEADER:
			matchPath.append("/")
					.append(request.getHeaders().getFirst(groupContext.getPredicate().getService().getKey()));
			break;
		case PATH:
		default:
			matchPath.append("/").append(pathSegments[index++]);
		}
		StringBuilder realPath = new StringBuilder();
		for (int i = index; i < pathSegments.length; i++) {
			matchPath.append("/").append(pathSegments[i]);
			realPath.append("/").append(pathSegments[i]);
		}
		if (path.endsWith("/")) {
			matchPath.append("/");
			realPath.append("/");
		}

		return new String[] {matchPath.toString(), realPath.toString()};
	}

	private void updateRouteMetadata(ServerWebExchange exchange, GroupContext.ContextRoute contextRoute) {
		if (CollectionUtils.isEmpty(contextRoute.getMetadata())) {
			return;
		}

		Route route = (Route) exchange.getAttributes().get(GATEWAY_ROUTE_ATTR);
		Constructor constructor = Route.class.getDeclaredConstructors()[1];
		constructor.setAccessible(true);
		try {
			HashMap<String, Object> metadata = new HashMap<>(route.getMetadata());
			metadata.putAll(contextRoute.getMetadata());
			Route newRoute = (Route) constructor.newInstance(route.getId(), route.getUri(),
					route.getOrder(), route.getPredicate(), route.getFilters(), metadata);
			exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, newRoute);
		}
		catch (Exception e) {
			logger.debug("[updateRouteMetadata] update route metadata failed", e);
		}
	}

	@Override
	public int getOrder() {
		// after RouteToRequestUrlFilter, DecodeTransferMetadataReactiveFilter
		return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 12;
	}
}
