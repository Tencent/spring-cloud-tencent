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
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.assembly.api.pojo.TraceAttributes;

public class TraceServerFinallyEnhancedPlugin implements EnhancedPlugin {

	private final PolarisSDKContextManager polarisSDKContextManager;

	private final List<SpanAttributesProvider> spanAttributesProviderList;

	public TraceServerFinallyEnhancedPlugin(PolarisSDKContextManager polarisSDKContextManager, List<SpanAttributesProvider> spanAttributesProviderList) {
		this.polarisSDKContextManager = polarisSDKContextManager;
		this.spanAttributesProviderList = spanAttributesProviderList;
	}

	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Server.FINALLY;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		Map<String, String> attributes = new HashMap<>();
		if (CollectionUtils.isNotEmpty(spanAttributesProviderList)) {
			for (SpanAttributesProvider spanAttributesProvider : spanAttributesProviderList) {
				Map<String, String> additionalAttributes = spanAttributesProvider.getServerFinallySpanAttributes(context);
				if (CollectionUtils.isNotEmpty(additionalAttributes)) {
					attributes.putAll(additionalAttributes);
				}
			}
		}

		TraceAttributes traceAttributes = new TraceAttributes();
		traceAttributes.setAttributes(attributes);
		traceAttributes.setAttributeLocation(TraceAttributes.AttributeLocation.SPAN);

		AssemblyAPI assemblyAPI = polarisSDKContextManager.getAssemblyAPI();
		assemblyAPI.updateTraceAttributes(traceAttributes);
	}

	@Override
	public int getOrder() {
		return PluginOrderConstant.ServerPluginOrder.TRACE_SERVER_PRE_PLUGIN_ORDER;
	}
}
