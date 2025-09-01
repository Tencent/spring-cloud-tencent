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

package com.tencent.cloud.rpc.enhancement.util;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.MetadataContextUtils;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.polaris.metadata.core.MetadataObjectValue;
import com.tencent.polaris.metadata.core.MetadataType;

/**
 * EnhancedPluginUtils.
 *
 * @author Shedfree Wu
 */
public final class EnhancedPluginUtils {

	private EnhancedPluginUtils() {
	}

	public static EnhancedPluginContext createEnhancedPluginContext() {
		EnhancedPluginContext context = new EnhancedPluginContext();
		MetadataContextUtils.putMetadataObjectValue(ContextConstant.ENHANCED_PLUGIN_CONTEXT, context);
		return context;
	}

	public static EnhancedPluginContext getEnhancedPluginContextFromMetadataContext() {

		MetadataObjectValue<EnhancedPluginContext> enhancedPluginContextObject = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.CUSTOM, false).
				getMetadataValue(ContextConstant.ENHANCED_PLUGIN_CONTEXT);

		if (MetadataContextUtils.existMetadataValue(enhancedPluginContextObject)) {
			return enhancedPluginContextObject.getObjectValue().get();
		}
		return null;
	}
}
