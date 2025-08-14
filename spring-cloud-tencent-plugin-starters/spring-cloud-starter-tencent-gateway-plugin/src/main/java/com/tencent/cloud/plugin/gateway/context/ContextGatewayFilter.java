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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.gateway.core.TsfGatewayRequest;
import com.tencent.tsf.gateway.core.constant.GatewayConstant;
import com.tencent.tsf.gateway.core.constant.HeaderName;
import com.tencent.tsf.gateway.core.exception.TsfGatewayError;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import com.tencent.tsf.gateway.core.model.PluginDetail;
import com.tencent.tsf.gateway.core.model.PluginInfo;
import com.tencent.tsf.gateway.core.model.PluginPayload;
import com.tencent.tsf.gateway.core.plugin.GatewayPluginFactory;
import com.tencent.tsf.gateway.core.plugin.IGatewayPlugin;
import com.tencent.tsf.gateway.core.util.CookieUtil;
import com.tencent.tsf.gateway.core.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.tsf.core.TsfContext;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import static com.tencent.tsf.gateway.core.constant.GatewayConstant.TSF_GATEWAY_RATELIMIT_CONTEXT_TAG;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_PATH_CONTAINER_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static org.springframework.http.server.PathContainer.parsePath;


public class ContextGatewayFilter implements GatewayFilter, Ordered {

	private static final Logger logger = LoggerFactory.getLogger(ContextGatewayFilter.class);

	private final ContextGatewayPropertiesManager manager;

	private final ContextGatewayFilterFactory.Config config;

	public ContextGatewayFilter(ContextGatewayPropertiesManager manager, ContextGatewayFilterFactory.Config config) {
		this.manager = manager;
		this.config = config;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ResponseStatusException pathRewriteException = (ResponseStatusException) exchange.getAttributes()
				.get(GatewayConstant.PATH_REWRITE_EXCEPTION);
		if (pathRewriteException != null) {
			throw pathRewriteException;
		}
		// plugins will add metadata
		if (exchange.getAttributes().containsKey(MetadataConstant.HeaderName.METADATA_CONTEXT)) {
			MetadataContextHolder.set((MetadataContext) exchange.getAttributes().get(
					MetadataConstant.HeaderName.METADATA_CONTEXT));
		}
		// for tsf gateway rate-limit
		TsfContext.putTag(TSF_GATEWAY_RATELIMIT_CONTEXT_TAG, config.getGroup());

		GroupContext groupContext = manager.getGroups().get(config.getGroup());

		// auth
		AuthCheckUtil.signCheck(exchange, groupContext);

		// path can be changed in ContextRoutePredicateFactory
		PathContainer path = (PathContainer) exchange.getAttributes()
				.computeIfAbsent(GATEWAY_PREDICATE_PATH_CONTAINER_ATTR,
						s -> parsePath(exchange.getRequest().getURI().getRawPath()));

		ServerWebExchange apiRebuildExchange;

		if (ApiType.MS.equals(groupContext.getPredicate().getApiType())) {
			apiRebuildExchange = msFilter(exchange, chain, groupContext, path);
		}
		else {
			apiRebuildExchange = externalFilter(exchange, chain, path);
		}

		ServerWebExchange pluginRebuildExchange = doPlugins(apiRebuildExchange,
				(GroupContext.ContextRoute) exchange.getAttributes().get(GatewayConstant.CONTEXT_ROUTE));

		return chain.filter(pluginRebuildExchange);

	}

	private ServerWebExchange doPlugins(ServerWebExchange exchange, GroupContext.ContextRoute contextRoute) {
		List<PluginPayload> pluginPayloadList = pluginCheck(exchange, contextRoute);
		if (CollectionUtils.isEmpty(pluginPayloadList)) {
			return exchange;
		}

		ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
		HttpHeaders headers = exchange.getRequest().getHeaders();
		StringBuilder cookieStringBuilder = new StringBuilder();
		List<String> cookie = headers.get("Cookie");
		if (!org.springframework.util.CollectionUtils.isEmpty(cookie)) {
			cookieStringBuilder.append(cookie.get(0));
		}

		Map<String, String[]> queryParamFromPlugin = new HashMap<>();
		Map<String, String> headersMapFromPlugin = new HashMap<>();
		Map<String, String> responseHeadersMapFromPlugin = new HashMap<>();
		//添加queryParam与header
		for (PluginPayload pluginPayload : pluginPayloadList) {
			Map<String, String[]> parameterMap = pluginPayload.getParameterMap();
			Map<String, String> headersMap = pluginPayload.getRequestHeaders();
			Map<String, String> responseHeaderMap = pluginPayload.getResponseHeaders();
			Map<String, String> requestCookieMap = pluginPayload.getRequestCookies();

			//合并请求参数
			if (!org.springframework.util.CollectionUtils.isEmpty(parameterMap)) {
				queryParamFromPlugin.putAll(parameterMap);
			}

			//合并请求头参数
			if (!org.springframework.util.CollectionUtils.isEmpty(headersMap)) {
				headersMapFromPlugin.putAll(headersMap);
			}
			//合并cookie参数
			CookieUtil.buildCookie(cookieStringBuilder, requestCookieMap);

			//合并响应头参数
			if (!org.springframework.util.CollectionUtils.isEmpty(responseHeaderMap)) {
				responseHeadersMapFromPlugin.putAll(responseHeaderMap);
			}

		}
		//添加cookie至header
		headersMapFromPlugin.put("Cookie", cookieStringBuilder.toString());
		//重构请求参数
		URI newUri = addQueryParam(exchange.getRequest().getURI(), queryParamFromPlugin);
		builder.uri(newUri);
		builder.headers(httpHeader -> httpHeader.setAll(headersMapFromPlugin));
		//添加响应头至请求上下文
		if (CollectionUtils.isNotEmpty(responseHeadersMapFromPlugin)) {
			responseHeadersMapFromPlugin.forEach((k, v) -> exchange.getResponse().getHeaders().add(k, v));
		}
		return exchange.mutate().request(builder.build()).build();
	}

	private List<PluginPayload> pluginCheck(ServerWebExchange exchange, GroupContext.ContextRoute contextRoute) {

		List<PluginPayload> pluginPayloadList = new ArrayList<>();

		List<PluginDetail> pluginDetails = manager.getPluginDetails(config.getGroup(), contextRoute.getApiId());

		if (pluginDetails == null || pluginDetails.size() == 0) {
			if (logger.isDebugEnabled()) {
				logger.debug("[doPlugins] No plugin found for group: {}, path:{}, path id: {}", config.getGroup(), contextRoute.getPath(), contextRoute.getApiId());
			}
			return pluginPayloadList;
		}

		TsfGatewayRequest tsfGatewayRequest = buildTsfGatewayRequest(exchange);

		//遍历绑定的插件
		for (PluginDetail pluginDetail : pluginDetails) {
			PluginInfo pluginInfo = manager.getPluginInfo(pluginDetail.getId());
			//pluginInfo.check();
			//执行插件逻辑
			if (logger.isDebugEnabled()) {
				logger.debug("pluginInfo is : {}", pluginInfo);
			}
			IGatewayPlugin gatewayPluginExecutor;
			try {
				gatewayPluginExecutor = GatewayPluginFactory.getGatewayPluginExecutor(pluginInfo.getType());

			}
			catch (Throwable t) {
				//日志打印出原始异常
				logger.error("build pluginExecute error", t);
				throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_ERROR, "build pluginExecute error");
			}

			if (gatewayPluginExecutor == null) {
				logger.error("can not find pluginExecutor");
				return Collections.emptyList();
			}

			PluginPayload pluginPayload = gatewayPluginExecutor.invoke(pluginInfo, tsfGatewayRequest);
			if (logger.isDebugEnabled()) {
				logger.debug("plugin invoke result: {}", pluginPayload);
			}
			pluginPayloadList.add(pluginPayload);
		}
		return pluginPayloadList;

	}

	private ServerWebExchange externalFilter(ServerWebExchange exchange, GatewayFilterChain chain, PathContainer path) {
		ServerHttpRequest request = exchange.getRequest();
		String[] apis = rebuildExternalApi(request, path.value());
		GroupContext.ContextRoute contextRoute = manager.getGroupPathRoute(config.getGroup(), apis[0]);
		if (contextRoute == null) {
			String msg = String.format("[externalFilter] Can't find context route for group: %s, path: %s, origin path: %s", config.getGroup(), apis[0], path.value());
			logger.warn(msg);
			throw NotFoundException.create(true, msg);
		}
		updateRouteMetadata(exchange, contextRoute);
		exchange.getAttributes().put(GatewayConstant.CONTEXT_ROUTE, contextRoute);

		URI requestUri = URI.create(contextRoute.getHost() + apis[1]);
		exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUri);
		// to correct path
		ServerHttpRequest newRequest = request.mutate().path(apis[1]).build();
		return exchange.mutate().request(newRequest).build();
	}

	private ServerWebExchange msFilter(ServerWebExchange exchange, GatewayFilterChain chain, GroupContext groupContext, PathContainer path) {
		ServerHttpRequest request = exchange.getRequest();
		String[] apis = rebuildMsApi(request, groupContext, path.value());
		logger.debug("[msFilter] path:{}, apis: {}", path, apis);
		// check api
		GroupContext.ContextRoute contextRoute = manager.getGroupPathRoute(config.getGroup(), apis[0]);
		if (contextRoute == null) {
			String msg = String.format("[msFilter] Can't find context route for group: %s, path: %s, origin path: %s", config.getGroup(), apis[0], path.value());
			logger.warn(msg);
			throw NotFoundException.create(true, msg);
		}
		updateRouteMetadata(exchange, contextRoute);
		exchange.getAttributes().put(GatewayConstant.CONTEXT_ROUTE, contextRoute);

		MetadataContext metadataContext = exchange.getAttribute(
				MetadataConstant.HeaderName.METADATA_CONTEXT);
		if (metadataContext != null) {
			// priority: namespace id(tsf) > namespace name(polaris)
			String routeNamespace = StringUtils.isBlank(contextRoute.getNamespaceId()) ?
					contextRoute.getNamespace() : contextRoute.getNamespaceId();
			metadataContext.putFragmentContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
					MetadataConstant.POLARIS_TARGET_NAMESPACE, routeNamespace);
		}
		URI requestUri = URI.create("lb://" + contextRoute.getService() + apis[1]);
		exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUri);
		// to correct path
		ServerHttpRequest newRequest = request.mutate().path(apis[1]).build();
		return exchange.mutate().request(newRequest).build();
	}

	/**
	 * e.g. "/context/api/test" → [ "GET|/api/test", "/api/test"]
	 */
	String[] rebuildExternalApi(ServerHttpRequest request, String path) {
		String[] pathSegments = path.split("/");
		StringBuilder matchPath = new StringBuilder();
		StringBuilder realPath = new StringBuilder();
		int index = 2;
		matchPath.append(request.getMethodValue()).append("|");
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
	String[] rebuildMsApi(ServerHttpRequest request, GroupContext groupContext, String path) {
		String[] pathSegments = path.split("/");
		StringBuilder matchPath = new StringBuilder();
		int index = 2;
		matchPath.append(request.getMethodValue()).append("|");

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
			if (index < pathSegments.length - 1) {
				matchPath.append("/").append(pathSegments[index++]);
			}
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
			if (index < pathSegments.length - 1) {
				matchPath.append("/").append(pathSegments[index++]);
			}
			break;
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

	private TsfGatewayRequest buildTsfGatewayRequest(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		URI uri = request.getURI();
		TsfGatewayRequest gatewayRequest = new TsfGatewayRequest();
		gatewayRequest.setUri(uri);
		gatewayRequest.setMethod(request.getMethodValue());
		gatewayRequest.setHeaders(buildTsfGatewayRequestHeader(exchange));
		gatewayRequest.setParameterMap(buildTsfGatewayRequestParameter(request.getQueryParams()));
		gatewayRequest.setRequestHeaders(copyRequestHeader(request));
		gatewayRequest.setCookieMap(getCookieMap(request));

		return gatewayRequest;
	}

	private Map<String, String> buildTsfGatewayRequestHeader(ServerWebExchange exchange) {
		ServerHttpRequest servletRequest = exchange.getRequest();
		HttpHeaders headers = servletRequest.getHeaders();
		String traceId = exchange.getAttribute(HeaderName.TRACE_ID);

		if (StringUtils.isEmpty(traceId)) {
			traceId = IdGenerator.generate();
		}
		// 设置TraceId
		exchange.getAttributes().put(HeaderName.TRACE_ID, traceId);

		Map<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put(HeaderName.NODE, headers.getFirst(HeaderName.NODE));
		headerMap.put(HeaderName.APP_KEY, headers.getFirst(HeaderName.APP_KEY));
		headerMap.put(HeaderName.ALG, headers.getFirst(HeaderName.ALG));
		headerMap.put(HeaderName.SIGN, headers.getFirst(HeaderName.SIGN));
		headerMap.put(HeaderName.TRACE_ID, traceId);
		headerMap.put(HeaderName.NONCE, headers.getFirst(HeaderName.NONCE));
		String namespaceIdKey = HeaderName.NAMESPACE_ID;
		headerMap.put(namespaceIdKey, headers.getFirst(namespaceIdKey));
		return headerMap;
	}

	private Map<String, String[]> buildTsfGatewayRequestParameter(MultiValueMap<String, String> queryParams) {
		Map<String, String[]> map = new HashMap<>();
		if (queryParams != null) {
			for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
				List<String> value = entry.getValue();
				String[] newValue = new String[value.size()];
				map.put(entry.getKey(), value.toArray(newValue));
			}
		}
		return map;
	}

	private Map<String, String> copyRequestHeader(ServerHttpRequest request) {
		Map<String, String> headerMap = new LinkedHashMap<>();
		HttpHeaders headers = request.getHeaders();
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				headerMap.put(entry.getKey(), entry.getValue().get(0));
			}
		}
		return headerMap;
	}

	private Map<String, String> getCookieMap(ServerHttpRequest request) {
		Map<String, String> map = new LinkedHashMap<>();
		MultiValueMap<String, HttpCookie> cookie = request.getCookies();
		for (Map.Entry<String, List<HttpCookie>> entry : cookie.entrySet()) {
			List<HttpCookie> httpCookies = entry.getValue();
			if (!httpCookies.isEmpty()) {
				map.put(entry.getKey(), httpCookies.get(0).getValue());
			}
		}
		return map;
	}

	private URI addQueryParam(URI uri, Map<String, String[]> queryParam) {

		StringBuilder query = new StringBuilder();
		String originalQuery = uri.getRawQuery();

		if (org.springframework.util.StringUtils.hasText(originalQuery)) {
			query.append(originalQuery);
		}

		for (Map.Entry<String, String[]> entry : queryParam.entrySet()) {
			String[] values = entry.getValue();
			for (String value : values) {
				if (query.length() == 0 || query.charAt(query.length() - 1) != '&') {
					query.append('&');
				}

				try {
					String encodedValue = URLEncoder.encode(value, "UTF-8");
					query.append(entry.getKey());
					query.append('=');
					query.append(encodedValue);
				}
				catch (UnsupportedEncodingException e) {
					logger.warn("value {} cannot be encoded to UTF-8", value, e);
				}
			}
		}

		try {
			URI newUri = UriComponentsBuilder.fromUri(uri)
					.replaceQuery(query.toString())
					.build(true)
					.toUri();

			return newUri;
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Invalid URI query: \"" + query + "\"");
		}
	}
}
