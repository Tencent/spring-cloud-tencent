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

package com.tencent.cloud.polaris.context.config.extend.tsf;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.SdkVersion;
import com.tencent.cloud.common.constant.WarmupCons;
import com.tencent.cloud.common.spi.InstanceMetadataProvider;
import com.tencent.cloud.common.util.inet.PolarisInetUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;


/**
 * InstanceMetadataProvider for TSF.
 *
 * @author Hoatian Zhang
 */
public class TsfInstanceMetadataProvider implements InstanceMetadataProvider {

	private final TsfCoreProperties tsfCoreProperties;

	public TsfInstanceMetadataProvider(TsfCoreProperties tsfCoreProperties) {
		this.tsfCoreProperties = tsfCoreProperties;
	}

	@Override
	public Map<String, String> getMetadata() {
		HashMap<String, String> tsfMetadata = new HashMap<>();
		if (StringUtils.isNotBlank(tsfCoreProperties.getTsfApplicationId())) {
			tsfMetadata.put(TsfMetadataConstants.TSF_APPLICATION_ID, tsfCoreProperties.getTsfApplicationId());
		}

		if (StringUtils.isNotBlank(tsfCoreProperties.getTsfProgVersion())) {
			tsfMetadata.put(TsfMetadataConstants.TSF_PROG_VERSION, tsfCoreProperties.getTsfProgVersion());
		}

		if (StringUtils.isNotBlank(tsfCoreProperties.getTsfGroupId())) {
			tsfMetadata.put(TsfMetadataConstants.TSF_GROUP_ID, tsfCoreProperties.getTsfGroupId());
		}

		if (StringUtils.isNotBlank(tsfCoreProperties.getTsfNamespaceId())) {
			tsfMetadata.put(TsfMetadataConstants.TSF_NAMESPACE_ID, tsfCoreProperties.getTsfNamespaceId());
		}

		if (StringUtils.isNotBlank(tsfCoreProperties.getInstanceId())) {
			tsfMetadata.put(TsfMetadataConstants.TSF_INSTNACE_ID, tsfCoreProperties.getInstanceId());
		}

		if (StringUtils.isNotBlank(tsfCoreProperties.getTsfRegion())) {
			tsfMetadata.put(TsfMetadataConstants.TSF_REGION, tsfCoreProperties.getTsfRegion());
		}

		if (StringUtils.isNotBlank(tsfCoreProperties.getTsfZone())) {
			tsfMetadata.put(TsfMetadataConstants.TSF_ZONE, tsfCoreProperties.getTsfZone());
		}

		tsfMetadata.put(WarmupCons.TSF_START_TIME, String.valueOf(System.currentTimeMillis()));
		tsfMetadata.put(TsfMetadataConstants.TSF_SDK_VERSION, SdkVersion.get());
		String ipv4Address = PolarisInetUtils.getIpString(false);
		if (StringUtils.isNotBlank(ipv4Address)) {
			tsfMetadata.put(TsfMetadataConstants.TSF_ADDRESS_IPV4, ipv4Address);
		}
		String ipv6Address = PolarisInetUtils.getIpString(true);
		if (StringUtils.isNotBlank(ipv6Address)) {
			tsfMetadata.put(TsfMetadataConstants.TSF_ADDRESS_IPV6, ipv6Address);
		}
		return tsfMetadata;
	}
}
