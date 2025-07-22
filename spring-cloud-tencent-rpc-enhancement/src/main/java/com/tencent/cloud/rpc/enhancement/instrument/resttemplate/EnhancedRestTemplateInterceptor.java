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

package com.tencent.cloud.rpc.enhancement.instrument.resttemplate;

import java.io.IOException;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.util.EnhancedPluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

/**
 * Interceptor used for before calling plugin in RestTemplate.
 *
 * It needs to execute after {@link LoadBalancerInterceptor}, because LaneRouter may add calleeTransitiveHeaders.
 *
 * @author Haotian Zhang
 */
public class EnhancedRestTemplateInterceptor implements ClientHttpRequestInterceptor, Ordered {

	private static final Logger LOG = LoggerFactory.getLogger(EnhancedRestTemplateInterceptor.class);

	private final EnhancedPluginRunner pluginRunner;

	public EnhancedRestTemplateInterceptor(EnhancedPluginRunner pluginRunner) {
		this.pluginRunner = pluginRunner;
	}

	@Override
	public int getOrder() {
		return OrderConstant.Client.RestTemplate.ENHANCE_INTERCEPTOR_ORDER;
	}

	@Override
	public ClientHttpResponse intercept(@NonNull HttpRequest httpRequest, @NonNull byte[] bytes,
			@NonNull ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {


		EnhancedPluginContext enhancedPluginContext = EnhancedPluginUtils.getEnhancedPluginContextFromMetadataContext();
		if (enhancedPluginContext != null) {
			pluginRunner.run(EnhancedPluginType.Client.BEFORE_CALLING, enhancedPluginContext);
		}
		else {
			LOG.debug("No exist enhanced plugin context, request:{}", httpRequest.getURI());
		}

		return clientHttpRequestExecution.execute(httpRequest, bytes);
	}
}
