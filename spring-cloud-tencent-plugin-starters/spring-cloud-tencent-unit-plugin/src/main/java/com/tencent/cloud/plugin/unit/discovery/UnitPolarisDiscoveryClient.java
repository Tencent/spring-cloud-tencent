/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
