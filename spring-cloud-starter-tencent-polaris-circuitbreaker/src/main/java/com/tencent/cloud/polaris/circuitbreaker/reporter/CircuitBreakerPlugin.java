/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.circuitbreaker.reporter;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.polaris.circuitbreaker.PolarisCircuitBreaker;
import com.tencent.cloud.polaris.circuitbreaker.instrument.resttemplate.PolarisCircuitBreakerHttpResponse;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import com.tencent.cloud.rpc.enhancement.plugin.reporter.SuccessPolarisReporter;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.metadata.core.MetadataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import static com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant.ClientPluginOrder.CIRCUIT_BREAKER_REPORTER_PLUGIN_ORDER;

/**
 * CircuitBreakerPlugin, do circuit breaker in enhance plugin and record info into metadata.
 *
 * @author Shedfree Wu
 */
public class CircuitBreakerPlugin implements EnhancedPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(SuccessPolarisReporter.class);

	private CircuitBreakerFactory circuitBreakerFactory;

	public CircuitBreakerPlugin(CircuitBreakerFactory circuitBreakerFactory) {
		this.circuitBreakerFactory = circuitBreakerFactory;
	}

	@Override
	public String getName() {
		return CircuitBreakerPlugin.class.getName();
	}

	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.PRE;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {

		EnhancedRequestContext request = context.getRequest();
		EnhancedResponseContext response = context.getResponse();

		String governanceNamespace = MetadataContext.LOCAL_NAMESPACE;

		String host = request.getServiceUrl() != null ? request.getServiceUrl().getHost() : request.getUrl().getHost();
		String path = request.getServiceUrl() != null ? request.getServiceUrl().getPath() : request.getUrl().getPath();
		String httpMethod = request.getHttpMethod().name();

		CircuitBreaker circuitBreaker = circuitBreakerFactory.create(governanceNamespace + "#" + host + "#" + path + "#http#" + httpMethod);
		if (circuitBreaker instanceof PolarisCircuitBreaker) {
			PolarisCircuitBreaker polarisCircuitBreaker = (PolarisCircuitBreaker) circuitBreaker;
			putMetadataObjectValue(ContextConstant.CircuitBreaker.POLARIS_CIRCUIT_BREAKER, polarisCircuitBreaker);
			putMetadataObjectValue(ContextConstant.CircuitBreaker.CIRCUIT_BREAKER_START_TIME, System.currentTimeMillis());

			try {
				polarisCircuitBreaker.acquirePermission();
			}
			catch (CallAbortedException e) {
				LOG.debug("[CircuitBreakerPlugin] request is aborted. request service url=[{}]", request.getServiceUrl());
				if (e.getFallbackInfo() != null) {
					Object fallbackResponse = new PolarisCircuitBreakerHttpResponse(e.getFallbackInfo());
					putMetadataObjectValue(ContextConstant.CircuitBreaker.CIRCUIT_BREAKER_FALLBACK_HTTP_RESPONSE, fallbackResponse);
				}
				throw e;
			}
		}
	}

	@Override
	public void handlerThrowable(EnhancedPluginContext context, Throwable throwable) {
		LOG.error("SuccessCircuitBreakerReporter runs failed. context=[{}].",
				context, throwable);
	}

	@Override
	public int getOrder() {
		return CIRCUIT_BREAKER_REPORTER_PLUGIN_ORDER;
	}

	private void putMetadataObjectValue(String key, Object value) {
		MetadataContextHolder.get().getMetadataContainer(MetadataType.APPLICATION, true).
				putMetadataObjectValue(key, value);
	}
}
