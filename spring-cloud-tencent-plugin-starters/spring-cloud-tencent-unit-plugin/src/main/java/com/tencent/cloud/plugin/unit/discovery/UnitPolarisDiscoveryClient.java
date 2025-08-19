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

package com.tencent.cloud.plugin.unit.discovery;

import java.util.List;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.tsf.unit.core.TencentUnitContext;
import com.tencent.tsf.unit.core.TencentUnitManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.ServiceInstance;

public class UnitPolarisDiscoveryClient extends PolarisDiscoveryClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(UnitPolarisDiscoveryClient.class);

	private final PolarisDiscoveryClient delegate;

	public UnitPolarisDiscoveryClient(PolarisDiscoveryClient delegate) {
		super(null);
		this.delegate = delegate;
	}

	@Override
	public String description() {
		return delegate.description();
	}

	@Override
	public List<ServiceInstance> getInstances(String service) {
		if (TencentUnitManager.isEnable()) {
			String[] parts = service.split("/");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("[getInstance] service:{}, unit context:{}", service, TencentUnitContext.getOriginCompositeContextMap());
			}

			if (parts.length != 2) {
				String namespace = TencentUnitContext.getStringRouteTag(TencentUnitContext.CLOUD_SPACE_ROUTE_TARGET_NAMESPACE_ID);

				MetadataContext metadataContext = MetadataContextHolder.get();
				metadataContext.putFragmentContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
						MetadataConstant.POLARIS_TARGET_NAMESPACE, namespace);
				return delegate.getInstances(service);
			}
			else {
				MetadataContext metadataContext = MetadataContextHolder.get();
				metadataContext.putFragmentContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
						MetadataConstant.POLARIS_TARGET_NAMESPACE, parts[0]);

				return delegate.getInstances(parts[1]);
			}
		}
		else {
			return delegate.getInstances(service);
		}
	}

	@Override
	public List<String> getServices() {
		return delegate.getServices();
	}
}
