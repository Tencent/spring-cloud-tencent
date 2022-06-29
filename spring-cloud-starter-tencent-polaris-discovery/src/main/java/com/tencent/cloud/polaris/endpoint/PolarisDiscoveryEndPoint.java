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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import org.apache.commons.lang.StringUtils;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Endpoint of polaris discovery, include discovery properties and service instance.
 *
 * @author shuiqingliu
 */
@Endpoint(id = "polaris-discovery")
public class PolarisDiscoveryEndPoint {

	private PolarisDiscoveryProperties polarisDiscoveryProperties;

	private DiscoveryClient polarisDiscoveryClient;


	public PolarisDiscoveryEndPoint(PolarisDiscoveryProperties polarisDiscoveryProperties, DiscoveryClient polarisDiscoveryClient) {
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
		this.polarisDiscoveryClient = polarisDiscoveryClient;
	}

	@ReadOperation
	public Map<String, Object> polarisDiscovery(@Selector String serviceId) {
		Map<String, Object> polarisDisConveryInfo = new HashMap<>();
		polarisDisConveryInfo.put("PolarisDiscoveryProperties", polarisDiscoveryProperties);

		List<ServiceInstance> serviceInstanceInfoList = Collections.emptyList();

		if (StringUtils.isNotEmpty(serviceId)) {
			serviceInstanceInfoList = polarisDiscoveryClient.getInstances(serviceId);
			polarisDisConveryInfo.put("ServiceInstance", serviceInstanceInfoList);
			return polarisDisConveryInfo;
		}

		for (String service : polarisDiscoveryClient.getServices()) {
			serviceInstanceInfoList.addAll(polarisDiscoveryClient.getInstances(service));
		}

		polarisDisConveryInfo.put("ServiceInstance", serviceInstanceInfoList);
		return polarisDisConveryInfo;
	}
}
