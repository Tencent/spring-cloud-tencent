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

package com.tencent.cloud.polaris.router.interceptor;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.config.properties.PolarisNearByRouterProperties;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.polaris.plugins.router.nearby.NearbyRouter;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;

/**
 * Router request interceptor for nearby router.
 * @author lepdou 2022-07-06
 */
public class NearbyRouterRequestInterceptor implements RouterRequestInterceptor {

	private final PolarisNearByRouterProperties polarisNearByRouterProperties;

	public NearbyRouterRequestInterceptor(PolarisNearByRouterProperties polarisNearByRouterProperties) {
		this.polarisNearByRouterProperties = polarisNearByRouterProperties;
	}

	@Override
	public void apply(ProcessRoutersRequest request, PolarisRouterContext routerContext) {
		if (!polarisNearByRouterProperties.isEnabled()) {
			return;
		}

		Map<String, String> nearbyRouterMetadata = new HashMap<>();
		nearbyRouterMetadata.put(NearbyRouter.ROUTER_ENABLED, "true");

		request.addRouterMetadata(NearbyRouter.ROUTER_TYPE_NEAR_BY, nearbyRouterMetadata);
	}
}
