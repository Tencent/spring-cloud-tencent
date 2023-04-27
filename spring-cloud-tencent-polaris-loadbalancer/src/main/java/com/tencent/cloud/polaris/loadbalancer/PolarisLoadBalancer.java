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

package com.tencent.cloud.polaris.loadbalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.PollingServerListUpdater;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.pojo.PolarisServer;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import org.apache.commons.lang.StringUtils;

import org.springframework.util.CollectionUtils;

/**
 * Routing load balancer of polaris.
 *
 * @author Haotian Zhang
 */
public class PolarisLoadBalancer extends DynamicServerListLoadBalancer<Server> {
	private static final ThreadLocal<List<Server>> THREAD_CACHE_SERVERS = new ThreadLocal<>();

	private final ConsumerAPI consumerAPI;

	private final PolarisLoadBalancerProperties polarisLoadBalancerProperties;

	public PolarisLoadBalancer(IClientConfig config, IRule rule, IPing ping, ServerList<Server> serverList,
			ConsumerAPI consumerAPI, PolarisLoadBalancerProperties properties) {
		super(config, rule, ping, serverList, null, new PollingServerListUpdater());
		this.consumerAPI = consumerAPI;
		this.polarisLoadBalancerProperties = properties;
	}

	@Override
	public void addServers(List<Server> servers) {
		THREAD_CACHE_SERVERS.set(servers);
	}

	@Override
	public List<Server> getReachableServers() {
		// Get servers first from the thread context
		if (!CollectionUtils.isEmpty(THREAD_CACHE_SERVERS.get())) {
			return THREAD_CACHE_SERVERS.get();
		}
		return getReachableServersWithoutCache();
	}

	public List<Server> getReachableServersWithoutCache() {
		ServiceInstances serviceInstances;
		if (polarisLoadBalancerProperties.getDiscoveryType().equals(ContextConstant.POLARIS)) {
			serviceInstances = getPolarisDiscoveryServiceInstances();
		}
		else {
			serviceInstances = getExtendDiscoveryServiceInstances();
		}

		if (serviceInstances == null || CollectionUtils.isEmpty(serviceInstances.getInstances())) {
			return Collections.emptyList();
		}

		List<Server> servers = new LinkedList<>();
		for (Instance instance : serviceInstances.getInstances()) {
			servers.add(new PolarisServer(serviceInstances, instance));
		}

		return servers;
	}

	private ServiceInstances getPolarisDiscoveryServiceInstances() {
		return getAllInstances(MetadataContext.LOCAL_NAMESPACE, name).toServiceInstances();
	}

	private ServiceInstances getExtendDiscoveryServiceInstances() {
		List<Server> allServers = super.getAllServers();
		if (CollectionUtils.isEmpty(allServers)) {
			return null;
		}
		ServiceInstances serviceInstances;
		if (StringUtils.isBlank(name)) {
			throw new IllegalStateException(
					"PolarisLoadBalancer only Server with AppName or ServiceIdForDiscovery attribute");
		}
		ServiceKey serviceKey = new ServiceKey(MetadataContext.LOCAL_NAMESPACE, name);
		List<Instance> instances = new ArrayList<>(8);
		for (Server server : allServers) {
			DefaultInstance instance = new DefaultInstance();
			instance.setNamespace(MetadataContext.LOCAL_NAMESPACE);
			instance.setService(name);
			instance.setHealthy(server.isAlive());
			instance.setProtocol(server.getScheme());
			instance.setId(server.getId());
			instance.setHost(server.getHost());
			instance.setPort(server.getPort());
			instance.setZone(server.getZone());
			instance.setWeight(100);
			instances.add(instance);
		}
		serviceInstances = new DefaultServiceInstances(serviceKey, instances);
		return serviceInstances;
	}

	@Override
	public List<Server> getAllServers() {
		return getReachableServers();
	}

	/**
	 * Get a list of instances.
	 * @param namespace namespace
	 * @param serviceName service name
	 * @return list of instances
	 */
	public InstancesResponse getAllInstances(String namespace, String serviceName) {
		GetHealthyInstancesRequest request = new GetHealthyInstancesRequest();
		request.setNamespace(namespace);
		request.setService(serviceName);
		request.setIncludeCircuitBreakInstances(false);
		return consumerAPI.getHealthyInstances(request);
	}
}
