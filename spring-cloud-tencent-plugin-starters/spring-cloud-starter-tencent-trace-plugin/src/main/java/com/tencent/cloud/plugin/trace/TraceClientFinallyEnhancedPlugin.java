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

import com.tencent.cloud.plugin.trace.attribute.SpanAttributesProvider;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant;
import com.tencent.polaris.api.utils.ClassUtils;
import io.opentelemetry.context.Scope;

public class TraceClientFinallyEnhancedPlugin implements EnhancedPlugin {

	public TraceClientFinallyEnhancedPlugin() {
	}

	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.FINALLY;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		Object otScope = context.getExtraData().get(SpanAttributesProvider.OT_SCOPE_KEY);
		if (ClassUtils.isClassPresent("io.opentelemetry.context.Scope") && otScope instanceof Scope) {
			((Scope) otScope).close();
		}
	}

	@Override
	public int getOrder() {
		return PluginOrderConstant.ClientPluginOrder.TRACE_CLIENT_PLUGIN_ORDER;
	}
}
