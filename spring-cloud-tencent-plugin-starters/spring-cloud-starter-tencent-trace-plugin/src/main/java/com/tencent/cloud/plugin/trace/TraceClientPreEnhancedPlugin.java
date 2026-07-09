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

package com.tencent.cloud.plugin.trace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.plugin.trace.attribute.SpanAttributesProvider;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedContextKeys;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.assembly.api.pojo.TraceAttributes;

public class TraceClientPreEnhancedPlugin implements EnhancedPlugin {

	private final PolarisSDKContextManager polarisSDKContextManager;

	private final List<SpanAttributesProvider> spanAttributesProviderList;

	public TraceClientPreEnhancedPlugin(PolarisSDKContextManager polarisSDKContextManager, List<SpanAttributesProvider> spanAttributesProviderList) {
		this.polarisSDKContextManager = polarisSDKContextManager;
		this.spanAttributesProviderList = spanAttributesProviderList;
	}

	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.BEFORE_CALLING;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		Map<String, String> attributes = new HashMap<>();
		if (CollectionUtils.isNotEmpty(spanAttributesProviderList)) {
			for (SpanAttributesProvider spanAttributesProvider : spanAttributesProviderList) {
				Map<String, String> additionalAttributes = spanAttributesProvider.getClientBaggageAttributes(context);
				if (CollectionUtils.isNotEmpty(additionalAttributes)) {
					attributes.putAll(additionalAttributes);
				}
			}
		}
		if (CollectionUtils.isEmpty(attributes)) {
			return;
		}

		// Reactive scenarios (SCG / WebClient): only stage the baggage attributes to be written; the
		// downstream interceptor injects them as a W3C baggage header on the outgoing request, so no
		// OTel Scope is attached here and there is no ThreadLocal lifecycle to close across async
		// boundaries.
		if (isReactiveScenario(context)) {
			context.getExtraData().put(EnhancedContextKeys.PENDING_BAGGAGE_ATTRIBUTES_KEY, attributes);
			return;
		}

		// Blocking scenarios (Feign / RestTemplate): keep the original semantics, attaching to the
		// current thread so the FINALLY stage can close it.
		TraceAttributes traceAttributes = new TraceAttributes();
		traceAttributes.setAttributes(attributes);
		traceAttributes.setAttributeLocation(TraceAttributes.AttributeLocation.BAGGAGE);

		AssemblyAPI assemblyAPI = polarisSDKContextManager.getAssemblyAPI();
		assemblyAPI.updateTraceAttributes(traceAttributes);
		Object otScope = traceAttributes.getOtScope();
		if (otScope != null) {
			context.getExtraData().put(SpanAttributesProvider.OT_SCOPE_KEY, otScope);
		}
	}

	/**
	 * Whether current invocation is a reactive client scenario (SCG / WebClient). Matched by class
	 * name prefix to avoid a hard dependency on spring-webflux / spring-cloud-gateway.
	 *
	 * @param context enhanced plugin context
	 * @return true if the current call is a reactive scenario
	 */
	private boolean isReactiveScenario(EnhancedPluginContext context) {
		Object origin = context.getOriginRequest();
		if (origin == null) {
			return false;
		}
		String className = origin.getClass().getName();
		// SCG: org.springframework.web.server.ServerWebExchange implementation
		// WebClient: org.springframework.web.reactive.function.client.ClientRequest implementation
		return className.startsWith("org.springframework.web.server.")
				|| className.startsWith("org.springframework.mock.web.server.")
				|| className.startsWith("org.springframework.web.reactive.function.client.")
				|| className.startsWith("org.springframework.cloud.gateway.");
	}

	@Override
	public int getOrder() {
		return PluginOrderConstant.ClientPluginOrder.TRACE_CLIENT_PLUGIN_ORDER;
	}
}
