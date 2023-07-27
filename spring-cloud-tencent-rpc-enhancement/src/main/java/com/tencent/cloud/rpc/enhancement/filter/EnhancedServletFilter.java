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

package com.tencent.cloud.rpc.enhancement.filter;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * EnhancedServletFilter.
 *
 * @author sean yu
 */
@Order(OrderConstant.Server.Servlet.ENHANCED_FILTER_ORDER)
public class EnhancedServletFilter extends OncePerRequestFilter {

	private final EnhancedPluginRunner pluginRunner;

	public EnhancedServletFilter(EnhancedPluginRunner pluginRunner) {
		this.pluginRunner = pluginRunner;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		EnhancedPluginContext enhancedPluginContext = new EnhancedPluginContext();

		HttpHeaders requestHeaders = new HttpHeaders();
		Enumeration<String> requestHeaderNames = request.getHeaderNames();
		if (requestHeaderNames != null) {
			while (requestHeaderNames.hasMoreElements()) {
				String requestHeaderName = requestHeaderNames.nextElement();
				requestHeaders.addAll(requestHeaderName, Collections.list(request.getHeaders(requestHeaderName)));
			}
		}
		EnhancedRequestContext enhancedRequestContext = EnhancedRequestContext.builder()
				.httpHeaders(requestHeaders)
				.httpMethod(HttpMethod.valueOf(request.getMethod()))
				.url(URI.create(request.getRequestURL().toString()))
				.build();
		enhancedPluginContext.setRequest(enhancedRequestContext);

		enhancedPluginContext.setLocalServiceInstance(pluginRunner.getLocalServiceInstance());

		// Run pre enhanced plugins.
		pluginRunner.run(EnhancedPluginType.Server.PRE, enhancedPluginContext);

		long startMillis = System.currentTimeMillis();
		try {
			filterChain.doFilter(request, response);
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);

			HttpHeaders responseHeaders = new HttpHeaders();
			Collection<String> responseHeaderNames = response.getHeaderNames();
			if (responseHeaderNames != null) {
				for (String responseHeaderName : responseHeaderNames) {
					responseHeaders.addAll(responseHeaderName, new ArrayList<>(response.getHeaders(responseHeaderName)));
				}
			}
			EnhancedResponseContext enhancedResponseContext = EnhancedResponseContext.builder()
					.httpStatus(response.getStatus())
					.httpHeaders(responseHeaders)
					.build();
			enhancedPluginContext.setResponse(enhancedResponseContext);

			// Run post enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Server.POST, enhancedPluginContext);
		}
		catch (ServletException | IOException e) {
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);
			enhancedPluginContext.setThrowable(e);
			// Run exception enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Server.EXCEPTION, enhancedPluginContext);
			throw e;
		}
		finally {
			// Run finally enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Server.FINALLY, enhancedPluginContext);
		}
	}

}
