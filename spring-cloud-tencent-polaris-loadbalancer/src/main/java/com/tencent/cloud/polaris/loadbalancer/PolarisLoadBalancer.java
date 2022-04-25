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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.PollingServerListUpdater;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.constant.MetadataConstant.SystemMetadataKey;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.pojo.PolarisServer;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Routing load balancer of polaris.
 *
 * @author Haotian Zhang
 */
public class PolarisLoadBalancer extends DynamicServerListLoadBalancer<Server> {

	private final RouterAPI routerAPI;

	private ConsumerAPI consumerAPI;

	private PolarisLoadBalancerProperties polarisLoadBalancerProperties;

	public PolarisLoadBalancer(IClientConfig config, IRule rule, IPing ping, ServerList<Server> serverList,
			RouterAPI routerAPI, ConsumerAPI consumerAPI, PolarisLoadBalancerProperties properties) {
		super(config, rule, ping, serverList, null, new PollingServerListUpdater());
		this.routerAPI = routerAPI;
		this.consumerAPI = consumerAPI;
		this.polarisLoadBalancerProperties = properties;
	}

	@Override
	public List<Server> getReachableServers() {
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
		ProcessRoutersRequest processRoutersRequest = new ProcessRoutersRequest();
		processRoutersRequest.setDstInstances(serviceInstances);
		String srcNamespace = MetadataContext.LOCAL_NAMESPACE;
		String srcService = MetadataContext.LOCAL_SERVICE;
		Map<String, String> transitiveCustomMetadata = MetadataContextHolder.get()
				.getAllTransitiveCustomMetadata();
		String method = MetadataContextHolder.get()
				.getSystemMetadata(SystemMetadataKey.PEER_PATH);
		processRoutersRequest.setMethod(method);
		if (StringUtils.isNotBlank(srcNamespace) && StringUtils.isNotBlank(srcService)) {
			ServiceInfo serviceInfo = new ServiceInfo();
			serviceInfo.setNamespace(srcNamespace);
			serviceInfo.setService(srcService);
			serviceInfo.setMetadata(transitiveCustomMetadata);
			processRoutersRequest.setSourceService(serviceInfo);
		}
		ProcessRoutersResponse processRoutersResponse = routerAPI
				.processRouters(processRoutersRequest);
		ServiceInstances filteredServiceInstances = processRoutersResponse
				.getServiceInstances();
		List<Server> filteredInstances = new ArrayList<>();
		for (Instance instance : filteredServiceInstances.getInstances()) {
			filteredInstances.add(new PolarisServer(serviceInstances, instance));
		}
		return filteredInstances;
	}

	private ServiceInstances getPolarisDiscoveryServiceInstances() {
		String serviceName = MetadataContextHolder.get().getSystemMetadata(SystemMetadataKey.PEER_SERVICE);
		if (StringUtils.isBlank(serviceName)) {
			List<Server> allServers = super.getAllServers();
			if (CollectionUtils.isEmpty(allServers)) {
				return null;
			}
			serviceName = ((PolarisServer) super.getAllServers().get(0)).getServiceInstances().getService();
		}
		return getAllInstances(MetadataContext.LOCAL_NAMESPACE, serviceName).toServiceInstances();
	}

	private ServiceInstances getExtendDiscoveryServiceInstances() {
		List<Server> allServers = super.getAllServers();
		if (CollectionUtils.isEmpty(allServers)) {
			return null;
		}
		ServiceInstances serviceInstances;
		String serviceName;
		// notice the difference between different service registries
		if (StringUtils.isNotBlank(
				allServers.get(0).getMetaInfo().getServiceIdForDiscovery())) {
			serviceName = allServers.get(0).getMetaInfo().getServiceIdForDiscovery();
		}
		else {
			serviceName = allServers.get(0).getMetaInfo().getAppName();
		}
		if (StringUtils.isBlank(serviceName)) {
			throw new IllegalStateException(
					"PolarisLoadBalancer only Server with AppName or ServiceIdForDiscovery attribute");
		}
		ServiceKey serviceKey = new ServiceKey(MetadataContext.LOCAL_NAMESPACE,
				serviceName);
		List<Instance> instances = new ArrayList<>(8);
		for (Server server : allServers) {
			DefaultInstance instance = new DefaultInstance();
			instance.setNamespace(MetadataContext.LOCAL_NAMESPACE);
			instance.setService(serviceName);
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
		GetAllInstancesRequest request = new GetAllInstancesRequest();
		request.setNamespace(namespace);
		request.setService(serviceName);
		return consumerAPI.getAllInstance(request);
	}

}
