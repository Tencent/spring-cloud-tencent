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

package com.tencent.cloud.polaris.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.tencent.cloud.common.pojo.PolarisServiceInstance;
import com.tencent.cloud.common.util.DiscoveryUtil;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.extend.nacos.NacosContextProperties;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.InstancesResponse;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.CollectionUtils;

/**
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
public class PolarisServiceDiscovery {

	private final NacosContextProperties nacosContextProperties;

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	private final PolarisDiscoveryHandler polarisDiscoveryHandler;

	public PolarisServiceDiscovery(
			NacosContextProperties nacosContextProperties,
			PolarisDiscoveryProperties polarisDiscoveryProperties,
			PolarisDiscoveryHandler polarisDiscoveryHandler) {
		this.nacosContextProperties = nacosContextProperties;
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
		this.polarisDiscoveryHandler = polarisDiscoveryHandler;
	}

	/**
	 * Return all instances for the given service.
	 * @param serviceId id of service
	 * @return list of instances
	 * @throws PolarisException polarisException
	 */
	public List<ServiceInstance> getInstances(String serviceId) throws PolarisException {
		serviceId = DiscoveryUtil.rewriteServiceId(serviceId);
		List<ServiceInstance> instances = new ArrayList<>();
		InstancesResponse filteredInstances = polarisDiscoveryHandler.getHealthyInstances(serviceId);
		ServiceInstances serviceInstances = filteredInstances.toServiceInstances();
		for (Instance instance : serviceInstances.getInstances()) {
			instances.add(new PolarisServiceInstance(instance));
		}
		return instances;
	}

	/**
	 * Return the names of all services.
	 * @return list of service names
	 * @throws PolarisException polarisException
	 */
	public List<String> getServices() throws PolarisException {
		if (CollectionUtils.isEmpty(polarisDiscoveryHandler.getServices().getServices())) {
			return Collections.emptyList();
		}
		return polarisDiscoveryHandler.getServices().getServices().stream()
				.map(ServiceInfo::getService).collect(Collectors.toList());
	}

}
