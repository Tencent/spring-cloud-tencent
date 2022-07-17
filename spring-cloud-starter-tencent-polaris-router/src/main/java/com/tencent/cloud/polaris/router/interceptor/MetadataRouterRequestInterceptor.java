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

import java.util.Map;
import java.util.Set;

import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.config.properties.PolarisMetadataRouterProperties;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.polaris.plugins.router.metadata.MetadataRouter;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;

/**
 * Router request interceptor for metadata router.
 * @author lepdou 2022-07-06
 */
public class MetadataRouterRequestInterceptor implements RouterRequestInterceptor {
	private static final String LABEL_KEY_METADATA_ROUTER_KEYS = "system-metadata-router-keys";

	private final PolarisMetadataRouterProperties polarisMetadataRouterProperties;

	public MetadataRouterRequestInterceptor(PolarisMetadataRouterProperties polarisMetadataRouterProperties) {
		this.polarisMetadataRouterProperties = polarisMetadataRouterProperties;
	}

	@Override
	public void apply(ProcessRoutersRequest request, PolarisRouterContext routerContext) {
		if (!polarisMetadataRouterProperties.isEnabled()) {
			return;
		}

		// 1. get metadata router label keys
		Set<String> metadataRouterKeys = routerContext.getLabelAsSet(LABEL_KEY_METADATA_ROUTER_KEYS);
		// 2. get metadata router labels
		Map<String, String> metadataRouterLabels = routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS,
				metadataRouterKeys);
		// 3. set metadata router labels to request
		request.addRouterMetadata(MetadataRouter.ROUTER_TYPE_METADATA, metadataRouterLabels);
	}
}
