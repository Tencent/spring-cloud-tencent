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

import java.util.ArrayList;
import java.util.Collection;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import static com.tencent.cloud.common.constant.ContextConstant.Zuul.POLARIS_PRE_ROUTE_TIME;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ERROR_TYPE;

/**
 * Polaris circuit breaker error-processing. Including reporting and fallback.
 * This will remove throwable from Zuul context.
 *
 * @author Haotian Zhang
 */
public class EnhancedErrorZuulFilter extends ZuulFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedErrorZuulFilter.class);

	private final EnhancedPluginRunner pluginRunner;

	private final Environment environment;

	public EnhancedErrorZuulFilter(EnhancedPluginRunner pluginRunner, Environment environment) {
		this.pluginRunner = pluginRunner;
		this.environment = environment;
	}

	@Override
	public String filterType() {
		return ERROR_TYPE;
	}

	@Override
	public int filterOrder() {
		return Integer.MIN_VALUE;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		String enabled = environment.getProperty("spring.cloud.tencent.rpc-enhancement.reporter");
		return ctx.getThrowable() != null && (StringUtils.isEmpty(enabled) || enabled.equals("true"));
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

		Object startTimeMilliObject = context.get(POLARIS_PRE_ROUTE_TIME);
		Throwable throwable = context.getThrowable();
		if (throwable != null && startTimeMilliObject != null && startTimeMilliObject instanceof Long) {
			HttpHeaders responseHeaders = new HttpHeaders();
			Collection<String> names = context.getResponse().getHeaderNames();
			for (String name : names) {
				responseHeaders.put(name, new ArrayList<>(context.getResponse().getHeaders(name)));
			}
			EnhancedResponseContext enhancedResponseContext = EnhancedResponseContext.builder()
					.httpStatus(context.getResponse().getStatus())
					.httpHeaders(responseHeaders)
					.build();
			enhancedPluginContext.setResponse(enhancedResponseContext);
			Long startTimeMilli = (Long) startTimeMilliObject;
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startTimeMilli);
			enhancedPluginContext.setThrowable(throwable);

			// Run post enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.EXCEPTION, enhancedPluginContext);

			// Run finally enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.FINALLY, enhancedPluginContext);
		}
		return null;
	}
}
