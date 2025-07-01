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
import java.util.Collections;
import java.util.Enumeration;

import com.netflix.zuul.context.RequestContext;
import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.util.ZuulFilterUtils;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static com.tencent.cloud.common.constant.ContextConstant.Zuul.POLARIS_PRE_ROUTE_TIME;

/**
 * Polaris circuit breaker implement in Zuul.
 *
 * @author Haotian Zhang
 */
public class EnhancedZuulPluginRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedZuulPluginRunner.class);

	private final EnhancedPluginRunner pluginRunner;

	public EnhancedZuulPluginRunner(EnhancedPluginRunner pluginRunner) {
		this.pluginRunner = pluginRunner;
	}

	public void run() {
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
					.serviceUrl(uri)
					.build();

			enhancedPluginContext.setRequest(enhancedRequestContext);
			enhancedPluginContext.setOriginRequest(context);
			enhancedPluginContext.setLocalServiceInstance(pluginRunner.getLocalServiceInstance());
			context.set(POLARIS_PRE_ROUTE_TIME, System.currentTimeMillis());

			// Run pre enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.PRE, enhancedPluginContext);
		}
		catch (URISyntaxException e) {
			LOGGER.error("Generate URI failed.", e);
		}
	}
}
