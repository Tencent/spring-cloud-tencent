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

package com.tencent.cloud.rpc.enhancement.plugin;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.polaris.client.api.SDKContext;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.util.CollectionUtils;

/**
 * Default plugin runner.
 *
 * @author Derek Yi 2022-08-16
 */
public class DefaultEnhancedPluginRunner implements EnhancedPluginRunner {

	private final Multimap<EnhancedPluginType, EnhancedPlugin> pluginMap = ArrayListMultimap.create();

	private final ServiceInstance localServiceInstance;

	public DefaultEnhancedPluginRunner(
			List<EnhancedPlugin> enhancedPlugins,
			Registration registration,
			SDKContext sdkContext
	) {
		if (!CollectionUtils.isEmpty(enhancedPlugins)) {
			enhancedPlugins.stream()
					.sorted(Comparator.comparing(EnhancedPlugin::getOrder))
					.forEach(plugin -> pluginMap.put(plugin.getType(), plugin));
		}
		if (registration != null) {
			localServiceInstance = registration;
		}
		else {
			DefaultServiceInstance defaultServiceInstance = new DefaultServiceInstance();
			defaultServiceInstance.setServiceId(MetadataContext.LOCAL_SERVICE);
			defaultServiceInstance.setHost(sdkContext.getConfig().getGlobal().getAPI().getBindIP());
			localServiceInstance = defaultServiceInstance;
		}
	}

	/**
	 * run the plugin.
	 *
	 * @param pluginType type of plugin
	 * @param context context in enhanced feign client.
	 */
	@Override
	public void run(EnhancedPluginType pluginType, EnhancedPluginContext context) {
		for (EnhancedPlugin plugin : pluginMap.get(pluginType)) {
			try {
				plugin.run(context);
			}
			catch (Throwable throwable) {
				plugin.handlerThrowable(context, throwable);
			}
		}
	}

	@Override
	public ServiceInstance getLocalServiceInstance() {
		return this.localServiceInstance;
	}

}
