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

package com.tencent.cloud.rpc.enhancement.resttemplate;

import java.io.IOException;
import java.util.Map;

import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import static com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType.EXCEPTION;
import static com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType.FINALLY;
import static com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType.POST;
import static com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType.PRE;

/**
 * EnhancedRestTemplate.
 *
 * @author sean yu
 */
public class EnhancedRestTemplate implements ClientHttpRequestInterceptor {

	private final EnhancedPluginRunner pluginRunner;

	public EnhancedRestTemplate(EnhancedPluginRunner pluginRunner) {
		this.pluginRunner = pluginRunner;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

		EnhancedPluginContext enhancedPluginContext = new EnhancedPluginContext();

		EnhancedRequestContext enhancedRequestContext = EnhancedRequestContext.builder()
				.httpHeaders(request.getHeaders())
				.httpMethod(request.getMethod())
				.url(request.getURI())
				.build();
		enhancedPluginContext.setRequest(enhancedRequestContext);

		// Run pre enhanced plugins.
		pluginRunner.run(PRE, enhancedPluginContext);
		long startMillis = System.currentTimeMillis();
		try {
			ClientHttpResponse response = execution.execute(request, body);
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);

			EnhancedResponseContext enhancedResponseContext = EnhancedResponseContext.builder()
					.httpStatus(response.getRawStatusCode())
					.httpHeaders(response.getHeaders())
					.build();
			enhancedPluginContext.setResponse(enhancedResponseContext);

			Map<String, String> loadBalancerContext = MetadataContextHolder.get().getLoadbalancerMetadata();
			DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
			serviceInstance.setServiceId(request.getURI().getHost());
			serviceInstance.setHost(loadBalancerContext.get(HeaderConstant.INTERNAL_CALLEE_INSTANCE_HOST));
			if (loadBalancerContext.get(HeaderConstant.INTERNAL_CALLEE_INSTANCE_PORT) != null) {
				serviceInstance.setPort(Integer.parseInt(loadBalancerContext.get(HeaderConstant.INTERNAL_CALLEE_INSTANCE_PORT)));
			}
			enhancedPluginContext.setServiceInstance(serviceInstance);

			// Run post enhanced plugins.
			pluginRunner.run(POST, enhancedPluginContext);
			return response;
		}
		catch (IOException e) {
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);
			enhancedPluginContext.setThrowable(e);
			// Run exception enhanced plugins.
			pluginRunner.run(EXCEPTION, enhancedPluginContext);
			throw e;
		}
		finally {
			// Run finally enhanced plugins.
			pluginRunner.run(FINALLY, enhancedPluginContext);
		}
	}

}
