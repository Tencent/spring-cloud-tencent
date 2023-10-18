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
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.util.ZuulFilterUtils;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * Polaris circuit breaker implement in Zuul.
 *
 * @author Haotian Zhang
 */
public class EnhancedPreZuulFilter extends ZuulFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedPreZuulFilter.class);

	private final EnhancedPluginRunner pluginRunner;

	private final Environment environment;

	public EnhancedPreZuulFilter(EnhancedPluginRunner pluginRunner, Environment environment) {
		this.pluginRunner = pluginRunner;
		this.environment = environment;
	}

	@Override
	public String filterType() {
		return PRE_TYPE;
	}

	@Override
	public int filterOrder() {
		return OrderConstant.Client.Zuul.ENHANCED_ROUTE_FILTER_ORDER;
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

			// Run pre enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.PRE, enhancedPluginContext);
		}
		catch (URISyntaxException e) {
			LOGGER.error("Generate URI failed.", e);
		}
		return null;
	}
}
