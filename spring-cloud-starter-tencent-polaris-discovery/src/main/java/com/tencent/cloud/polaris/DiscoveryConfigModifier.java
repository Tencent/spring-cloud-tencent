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

package com.tencent.cloud.polaris;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.router.healthy.RecoverRouterConfig;

/**
 * Spring Cloud Tencent config Override polaris config.
 *
 * @author lepdou 2022-04-24
 */
public class DiscoveryConfigModifier implements PolarisConfigModifier {

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	public DiscoveryConfigModifier(PolarisDiscoveryProperties polarisDiscoveryProperties) {
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		// Set excludeCircuitBreakInstances to false
		RecoverRouterConfig recoverRouterConfig = configuration.getConsumer().getServiceRouter()
				.getPluginConfig(ServiceRouterConfig.DEFAULT_ROUTER_RECOVER, RecoverRouterConfig.class);
		recoverRouterConfig.setExcludeCircuitBreakInstances(false);

		// Update modified config to source properties
		configuration.getConsumer().getServiceRouter()
				.setPluginConfig(ServiceRouterConfig.DEFAULT_ROUTER_RECOVER, recoverRouterConfig);

		// Set ServiceRefreshInterval
		configuration.getConsumer().getLocalCache()
				.setServiceListRefreshInterval(polarisDiscoveryProperties.getServiceListRefreshInterval());
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.DISCOVERY_ORDER;
	}
}
