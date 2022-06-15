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

package com.tencent.cloud.polaris.loadbalancer;

import java.util.ArrayList;
import java.util.List;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.pojo.PolarisServiceInstance;
import com.tencent.polaris.api.pojo.DefaultInstance;
import org.apache.commons.lang.StringUtils;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.DelegatingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.util.CollectionUtils;

/**
 * Service instance list supplier of Polaris.
 *
 * @author Haotian Zhang
 */
public class PolarisServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

	public PolarisServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
		super(delegate);
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return getDelegate().get().map(this::chooseInstances);
	}

	@Override
	public Flux<List<ServiceInstance>> get(Request request) {
		return super.get(request);
	}

	public List<ServiceInstance> chooseInstances(List<ServiceInstance> allServers) {
		if (CollectionUtils.isEmpty(allServers)) {
			return allServers;
		}

		String serviceName = allServers.get(0).getServiceId();
		if (StringUtils.isBlank(serviceName)) {
			throw new IllegalStateException(
					"PolarisRoutingLoadBalancer only Server with AppName or ServiceIdForDiscovery attribute");
		}
		List<ServiceInstance> serviceInstances = new ArrayList<>(allServers.size());
		for (ServiceInstance server : allServers) {
			DefaultInstance instance = new DefaultInstance();
			instance.setNamespace(MetadataContext.LOCAL_NAMESPACE);
			instance.setService(serviceName);
			instance.setProtocol(server.getScheme());
			instance.setId(server.getInstanceId());
			instance.setHost(server.getHost());
			instance.setPort(server.getPort());
			instance.setWeight(100);
			instance.setMetadata(server.getMetadata());
			serviceInstances.add(new PolarisServiceInstance(instance));
		}
		return serviceInstances;
	}

}
