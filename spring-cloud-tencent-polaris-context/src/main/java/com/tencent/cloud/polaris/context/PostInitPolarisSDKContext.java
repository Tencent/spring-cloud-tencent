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

package com.tencent.cloud.polaris.context;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;

/**
 * After all configurations are loaded, post-initialize SDKContext.
 *
 * @author lepdou 2022-06-28
 */
public class PostInitPolarisSDKContext {

	public PostInitPolarisSDKContext(SDKContext sdkContext, StaticMetadataManager staticMetadataManager) {
		// set instance's location info
		String region = staticMetadataManager.getRegion();
		String zone = staticMetadataManager.getZone();
		String campus = staticMetadataManager.getCampus();

		ValueContext valueContext = sdkContext.getValueContext();
		if (StringUtils.isNotBlank(region)) {
			valueContext.setValue(RoutingProto.NearbyRoutingConfig.LocationLevel.REGION.name(), region);
		}
		if (StringUtils.isNotBlank(zone)) {
			valueContext.setValue(RoutingProto.NearbyRoutingConfig.LocationLevel.ZONE.name(), zone);
		}
		if (StringUtils.isNotBlank(campus)) {
			valueContext.setValue(RoutingProto.NearbyRoutingConfig.LocationLevel.CAMPUS.name(), campus);
		}
	}
}
