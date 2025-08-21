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

package com.tencent.cloud.polaris.router.interceptor;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.config.properties.PolarisMetadataRouterProperties;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.plugins.router.metadata.MetadataRouter;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;

/**
 * Router request interceptor for metadata router.
 * @author lepdou, Hoatian Zhang
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
		// set metadata router label keys
		MetadataContainer metadataContainer = MetadataContextHolder.get()
				.getMetadataContainer(MetadataType.CUSTOM, false);
		String metadataRouteKeys = metadataContainer.getRawMetadataStringValue(LABEL_KEY_METADATA_ROUTER_KEYS);
		metadataContainer.putMetadataMapValue(MetadataRouter.ROUTER_TYPE_METADATA, MetadataRouter.KEY_METADATA_KEYS, metadataRouteKeys, TransitiveType.NONE);
		request.setMetadataFailoverType(polarisMetadataRouterProperties.getFailOver());
	}
}
