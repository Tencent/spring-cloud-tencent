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

package com.tencent.cloud.rpc.enhancement.instrument.zuul;

import java.net.URI;
import java.net.URISyntaxException;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.util.ZuulFilterUtils;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpResponse;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import static com.tencent.cloud.common.constant.ContextConstant.Zuul.POLARIS_PRE_ROUTE_TIME;
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
		return OrderConstant.Client.Zuul.ENHANCED_ROUTE_FILTER_ORDER;
	}

	@Override
	public boolean shouldFilter() {
		String enabled = environment.getProperty("spring.cloud.tencent.rpc-enhancement.reporter");
		return StringUtils.isEmpty(enabled) || enabled.equals("true");
	}

	@Override
	public Object run() throws ZuulException {
		RequestContext context = RequestContext.getCurrentContext();
		Object enhancedPluginContextObj = context.get(ContextConstant.Zuul.ENHANCED_PLUGIN_CONTEXT);
		EnhancedPluginContext enhancedPluginContext;
		if (enhancedPluginContextObj == null || !(enhancedPluginContextObj instanceof EnhancedPluginContext)) {
			enhancedPluginContext = new EnhancedPluginContext();
		}
		else {
			enhancedPluginContext = (EnhancedPluginContext) enhancedPluginContextObj;
		}
		try {
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
				URI uri = new URI(context.getRequest()
						.getScheme(), ZuulFilterUtils.getServiceId(context), ZuulFilterUtils.getPath(context), context.getRequest()
						.getQueryString(), null);
				enhancedPluginContext.setTargetServiceInstance(null, uri);
			}

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
