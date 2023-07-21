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

package com.tencent.cloud.polaris.discovery;

import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.GetServicesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServicesResponse;

/**
 * Discovery Handler for Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
public class PolarisDiscoveryHandler {

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	private final ConsumerAPI polarisConsumer;

	public PolarisDiscoveryHandler(PolarisDiscoveryProperties polarisDiscoveryProperties,
			PolarisSDKContextManager polarisSDKContextManager) {
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
		this.polarisConsumer = polarisSDKContextManager.getConsumerAPI();
	}

	/**
	 * Get a list of healthy instances.
	 * @param service service name
	 * @return list of healthy instances
	 */
	public InstancesResponse getHealthyInstances(String service) {
		String namespace = polarisDiscoveryProperties.getNamespace();
		GetHealthyInstancesRequest getHealthyInstancesRequest = new GetHealthyInstancesRequest();
		getHealthyInstancesRequest.setNamespace(namespace);
		getHealthyInstancesRequest.setService(service);
		getHealthyInstancesRequest.setIncludeCircuitBreakInstances(false);
		return polarisConsumer.getHealthyInstances(getHealthyInstancesRequest);
	}

	/**
	 * Return all instances for the given service.
	 * @param service serviceName
	 * @return list of instances
	 */
	public InstancesResponse getInstances(String service) {
		String namespace = polarisDiscoveryProperties.getNamespace();
		GetAllInstancesRequest request = new GetAllInstancesRequest();
		request.setNamespace(namespace);
		request.setService(service);
		return polarisConsumer.getAllInstances(request);
	}

	/**
	 * Return all service for given namespace.
	 * @return service list
	 */
	public ServicesResponse getServices() {
		String namespace = polarisDiscoveryProperties.getNamespace();
		GetServicesRequest request = new GetServicesRequest();
		request.setNamespace(namespace);
		return polarisConsumer.getServices(request);
	}
}
