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

package com.tencent.cloud.polaris.ribbon;

import java.util.ArrayList;
import java.util.List;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerList;
import com.netflix.loadbalancer.Server;
import com.tencent.cloud.common.pojo.PolarisServer;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.InstancesResponse;

/**
 * Server list of Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
public class PolarisServerList extends AbstractServerList<Server> {

	private String serviceId;

	private final PolarisDiscoveryHandler polarisDiscoveryHandler;

	public PolarisServerList(PolarisDiscoveryHandler polarisDiscoveryHandler) {
		this.polarisDiscoveryHandler = polarisDiscoveryHandler;
	}

	@Override
	public List<Server> getInitialListOfServers() {
		return getServers();
	}

	@Override
	public List<Server> getUpdatedListOfServers() {
		return getServers();
	}

	private List<Server> getServers() {
		InstancesResponse allInstances = polarisDiscoveryHandler.getHealthyInstances(serviceId);
		ServiceInstances serviceInstances = allInstances.toServiceInstances();
		List<Server> polarisServers = new ArrayList<>();
		for (Instance instance : serviceInstances.getInstances()) {
			polarisServers.add(new PolarisServer(serviceInstances, instance));
		}
		return polarisServers;
	}

	public String getServiceId() {
		return serviceId;
	}

	@Override
	public void initWithNiwsConfig(IClientConfig iClientConfig) {
		this.serviceId = iClientConfig.getClientName();
	}
}
