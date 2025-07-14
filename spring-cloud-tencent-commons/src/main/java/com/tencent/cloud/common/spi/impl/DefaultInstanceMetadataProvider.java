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

package com.tencent.cloud.common.spi.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.spi.InstanceMetadataProvider;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.inet.PolarisInetUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;

import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAME;
import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE;
import static com.tencent.cloud.common.metadata.MetadataContext.LOCAL_NAMESPACE;
import static com.tencent.cloud.common.metadata.MetadataContext.LOCAL_SERVICE;

/**
 * DefaultInstanceMetadataProvider.
 * provide DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE, DEFAULT_METADATA_SOURCE_SERVICE_NAME
 *
 * @author sean yu
 */
public class DefaultInstanceMetadataProvider implements InstanceMetadataProvider {

	private final ApplicationContextAwareUtils applicationContextAwareUtils;

	// ensure ApplicationContextAwareUtils init before
	public DefaultInstanceMetadataProvider(ApplicationContextAwareUtils applicationContextAwareUtils) {
		this.applicationContextAwareUtils = applicationContextAwareUtils;
	}

	@Override
	public Map<String, String> getMetadata() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put(DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE, LOCAL_NAMESPACE);
		metadata.put(DEFAULT_METADATA_SOURCE_SERVICE_NAME, LOCAL_SERVICE);
		String ipv4Address = PolarisInetUtils.getIpString(false);
		if (StringUtils.isNotBlank(ipv4Address)) {
			metadata.put(MetadataConstants.ADDRESS_IPV4, ipv4Address);
		}
		String ipv6Address = PolarisInetUtils.getIpString(true);
		if (StringUtils.isNotBlank(ipv6Address)) {
			metadata.put(MetadataConstants.ADDRESS_IPV6, ipv6Address);
		}
		return metadata;
	}

	@Override
	public Set<String> getDisposableMetadataKeys() {
		return new HashSet<>(Arrays.asList(DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE, DEFAULT_METADATA_SOURCE_SERVICE_NAME));
	}

}
