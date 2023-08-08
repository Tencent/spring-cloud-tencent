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
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import static com.tencent.cloud.common.constant.ContextConstant.Zuul.POLARIS_PRE_ROUTE_TIME;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;

/**
 * Polaris circuit breaker implement in Zuul.
 *
 * @author Haotian Zhang
 */
public class EnhancedPostZuulFilter extends ZuulFilter {

	private final EnhancedPluginRunner pluginRunner;

	private final Environment environment;

	public EnhancedPostZuulFilter(EnhancedPluginRunner pluginRunner, Environment environment) {
		this.pluginRunner = pluginRunner;
		this.environment = environment;
	}

	@Override
	public String filterType() {
		return POST_TYPE;
	}

	@Override
	public int filterOrder() {
		return OrderConstant.Client.Zuul.ENHANCED_POST_FILTER_ORDER;
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

		Object startTimeMilliObject = context.get(POLARIS_PRE_ROUTE_TIME);
		if (startTimeMilliObject instanceof Long) {
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

			// Run post enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.POST, enhancedPluginContext);

			// Run finally enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.FINALLY, enhancedPluginContext);
		}
		return null;
	}
}
