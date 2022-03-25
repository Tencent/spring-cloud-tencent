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

package com.tencent.cloud.polaris.router.rule;

import java.util.List;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.Server;
import com.tencent.cloud.common.pojo.PolarisServer;
import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

/**
 * Weighted random load balance strategy.
 *
 * @author Haotian Zhang
 */
public class PolarisWeightedRandomRule extends AbstractLoadBalancerRule {

	private static final String POLICY = LoadBalanceConfig.LOAD_BALANCE_WEIGHTED_RANDOM;

	@Autowired
	private RouterAPI polarisRouter;

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {

	}

	@Override
	public Server choose(Object key) {
		List<Server> allServers = getLoadBalancer().getReachableServers();
		if (CollectionUtils.isEmpty(allServers)) {
			return null;
		}
		Server server = allServers.get(0);
		if (!(server instanceof PolarisServer)) {
			throw new IllegalStateException(
					"PolarisDiscoveryRule only support PolarisServer instances");
		}
		PolarisServer polarisServer = (PolarisServer) server;
		ProcessLoadBalanceRequest request = new ProcessLoadBalanceRequest();
		request.setDstInstances(polarisServer.getServiceInstances());
		request.setLbPolicy(POLICY);
		ProcessLoadBalanceResponse processLoadBalanceResponse = polarisRouter
				.processLoadBalance(request);
		Instance targetInstance = processLoadBalanceResponse.getTargetInstance();
		return new PolarisServer(polarisServer.getServiceInstances(), targetInstance);
	}

}
