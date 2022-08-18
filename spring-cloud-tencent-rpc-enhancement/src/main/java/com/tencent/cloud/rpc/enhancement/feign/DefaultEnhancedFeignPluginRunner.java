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
 *
 */

package com.tencent.cloud.rpc.enhancement.feign;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignContext;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPlugin;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType;

import org.springframework.util.CollectionUtils;

/**
 * Default plugin runner.
 *
 * @author Derek Yi 2022-08-16
 */
public class DefaultEnhancedFeignPluginRunner implements EnhancedFeignPluginRunner {

	private Multimap<String, EnhancedFeignPlugin> pluginMap = ArrayListMultimap.create();

	public DefaultEnhancedFeignPluginRunner(List<EnhancedFeignPlugin> enhancedFeignPlugins) {
		if (!CollectionUtils.isEmpty(enhancedFeignPlugins)) {
			enhancedFeignPlugins.stream()
					.sorted(Comparator.comparing(EnhancedFeignPlugin::getOrder))
					.forEach(plugin -> pluginMap.put(plugin.getType().name(), plugin));
		}
	}

	/**
	 * run the plugin.
	 *
	 * @param pluginType type of plugin
	 * @param context context in enhanced feign client.
	 */
	@Override
	public void run(EnhancedFeignPluginType pluginType, EnhancedFeignContext context) {
		for (EnhancedFeignPlugin plugin : pluginMap.get(pluginType.name())) {
			try {
				plugin.run(context);
			}
			catch (Throwable throwable) {
				plugin.handlerThrowable(context, throwable);
			}
		}
	}
}
