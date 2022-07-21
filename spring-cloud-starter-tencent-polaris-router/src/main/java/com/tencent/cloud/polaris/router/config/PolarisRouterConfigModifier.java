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

package com.tencent.cloud.polaris.router.config;

import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.route.LocationLevel;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.router.nearby.NearbyRouterConfig;

/**
 * Recover polaris router plugin default configuration.
 * @author lepdou 2022-07-21
 */
public class PolarisRouterConfigModifier implements PolarisConfigModifier {

	@Override
	public void modify(ConfigurationImpl configuration) {
		// set default location level with campus.
		NearbyRouterConfig nearbyRouterConfig = configuration.getConsumer().getServiceRouter()
				.getPluginConfig(ServiceRouterConfig.DEFAULT_ROUTER_NEARBY, NearbyRouterConfig.class);

		nearbyRouterConfig.setMatchLevel(LocationLevel.campus);

		configuration.getConsumer().getServiceRouter()
				.setPluginConfig(ServiceRouterConfig.DEFAULT_ROUTER_NEARBY, nearbyRouterConfig);
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
