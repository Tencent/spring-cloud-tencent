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

package com.tencent.cloud.rpc.enhancement.zuul;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.util.ZuulFilterUtils;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import static com.tencent.cloud.common.constant.ContextConstant.Zuul.POLARIS_PRE_ROUTE_TIME;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.RIBBON_ROUTING_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;

/**
 * Polaris circuit breaker implement in Zuul.
 *
 * @author Haotian Zhang
 */
public class EnhancedRouteZuulFilter extends ZuulFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedRouteZuulFilter.class);

	private final EnhancedPluginRunner pluginRunner;

	private final Environment environment;

	public EnhancedRouteZuulFilter(EnhancedPluginRunner pluginRunner, Environment environment) {
		this.pluginRunner = pluginRunner;
		this.environment = environment;
	}

	@Override
	public String filterType() {
		return ROUTE_TYPE;
	}

	@Override
	public int filterOrder() {
		return RIBBON_ROUTING_FILTER_ORDER + 1;
	}

	@Override
	public boolean shouldFilter() {
		String enabled = environment.getProperty("spring.cloud.tencent.rpc-enhancement.reporter");
		return StringUtils.isEmpty(enabled) || enabled.equals("true");
	}

	@Override
	public Object run() throws ZuulException {
		EnhancedPluginContext enhancedPluginContext = new EnhancedPluginContext();
		RequestContext context = RequestContext.getCurrentContext();
		context.set(ContextConstant.Zuul.ENHANCED_PLUGIN_CONTEXT, enhancedPluginContext);

		try {
			URI uri = new URI(context.getRequest()
					.getScheme(), ZuulFilterUtils.getServiceId(context), ZuulFilterUtils.getPath(context), context.getRequest()
					.getQueryString(), null);
			HttpHeaders requestHeaders = new HttpHeaders();
			Enumeration<String> names = context.getRequest().getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				requestHeaders.put(name, Collections.list(context.getRequest().getHeaders(name)));
			}
			EnhancedRequestContext enhancedRequestContext = EnhancedRequestContext.builder()
					.httpHeaders(requestHeaders)
					.httpMethod(HttpMethod.resolve(context.getRequest().getMethod()))
					.url(uri)
					.build();

			enhancedPluginContext.setRequest(enhancedRequestContext);
			enhancedPluginContext.setLocalServiceInstance(pluginRunner.getLocalServiceInstance());

			Object ribbonResponseObj = context.get("ribbonResponse");
			RibbonApacheHttpResponse ribbonResponse;
			DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
			if (ribbonResponseObj != null && ribbonResponseObj instanceof RibbonApacheHttpResponse) {
				ribbonResponse = (RibbonApacheHttpResponse) ribbonResponseObj;
				serviceInstance.setServiceId(ZuulFilterUtils.getServiceId(context));
				serviceInstance.setHost(ribbonResponse.getRequestedURI().getHost());
				serviceInstance.setPort(ribbonResponse.getRequestedURI().getPort());
				enhancedPluginContext.setTargetServiceInstance(serviceInstance, null);
			}
			else {
				enhancedPluginContext.setTargetServiceInstance(null, uri);
			}

			// Run pre enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.PRE, enhancedPluginContext);

			Object startTimeMilliObject = context.get(POLARIS_PRE_ROUTE_TIME);
			if (startTimeMilliObject == null || !(startTimeMilliObject instanceof Long)) {
				context.set(POLARIS_PRE_ROUTE_TIME, Long.valueOf(System.currentTimeMillis()));
			}
		}
		catch (URISyntaxException e) {
			LOGGER.error("Generate URI failed.", e);
		}
		return null;
	}
}
