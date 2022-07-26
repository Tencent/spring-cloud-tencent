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

package com.tencent.cloud.polaris.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.InstancesResponse;
import org.apache.commons.lang.StringUtils;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Endpoint of polaris discovery, include discovery properties and service instance.
 *
 * @author shuiqingliu
 */
@Endpoint(id = "polaris-discovery")
public class PolarisDiscoveryEndpoint {

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;
	private final DiscoveryClient polarisDiscoveryClient;
	private final PolarisDiscoveryHandler polarisDiscoveryHandler;

	public PolarisDiscoveryEndpoint(PolarisDiscoveryProperties polarisDiscoveryProperties, DiscoveryClient polarisDiscoveryClient, PolarisDiscoveryHandler polarisDiscoveryHandler) {
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
		this.polarisDiscoveryClient = polarisDiscoveryClient;
		this.polarisDiscoveryHandler = polarisDiscoveryHandler;
	}

	@ReadOperation
	public Map<String, Object> polarisDiscovery(@Selector String serviceId) {
		Map<String, Object> polarisDiscoveryInfo = new HashMap<>();
		polarisDiscoveryInfo.put("PolarisDiscoveryProperties", polarisDiscoveryProperties);

		List<ServiceInstances> serviceInstancesInfoList = new ArrayList<>();

		if (StringUtils.isNotEmpty(serviceId)) {
			ServiceInstances serviceInstances = getServiceInstances(serviceId);
			serviceInstancesInfoList.add(serviceInstances);
			polarisDiscoveryInfo.put("ServiceInstances", serviceInstancesInfoList);
			return polarisDiscoveryInfo;
		}

		for (String service : polarisDiscoveryClient.getServices()) {
			ServiceInstances serviceInstances = getServiceInstances(service);
			serviceInstancesInfoList.add(serviceInstances);
		}

		polarisDiscoveryInfo.put("ServiceInstances", serviceInstancesInfoList);
		return polarisDiscoveryInfo;
	}

	private ServiceInstances getServiceInstances(String serviceId) {
		InstancesResponse instancesResponse = polarisDiscoveryHandler.getHealthyInstances(serviceId);
		return instancesResponse.toServiceInstances();
	}
}
