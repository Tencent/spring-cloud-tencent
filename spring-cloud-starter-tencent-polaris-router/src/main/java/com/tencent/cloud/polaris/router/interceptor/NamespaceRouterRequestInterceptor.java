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
import com.tencent.cloud.polaris.router.config.properties.PolarisNamespaceRouterProperties;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.plugins.router.namespace.NamespaceRouter;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;

/**
 * Router request interceptor for namespace router.
 *
 * @author Hoatian Zhang
 */
public class NamespaceRouterRequestInterceptor implements RouterRequestInterceptor {

	private final PolarisNamespaceRouterProperties polarisNamespaceRouterProperties;

	public NamespaceRouterRequestInterceptor(PolarisNamespaceRouterProperties polarisNamespaceRouterProperties) {
		this.polarisNamespaceRouterProperties = polarisNamespaceRouterProperties;
	}

	@Override
	public void apply(ProcessRoutersRequest request, PolarisRouterContext routerContext) {
		// set namespace router enable
		MetadataContainer metadataContainer = MetadataContextHolder.get()
				.getMetadataContainer(MetadataType.CUSTOM, false);
		metadataContainer.putMetadataMapValue(NamespaceRouter.ROUTER_TYPE_NAMESPACE, NamespaceRouter.ROUTER_ENABLED,
				String.valueOf(polarisNamespaceRouterProperties.isEnabled()), TransitiveType.NONE);

		// set namespace router fail over type.
		request.setNamespaceRouterFailoverType(polarisNamespaceRouterProperties.getFailOver());
	}
}
