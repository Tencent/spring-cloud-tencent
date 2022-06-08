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

package com.tencent.cloud.polaris.router;

import java.util.LinkedList;
import java.util.List;

import com.netflix.loadbalancer.Server;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * test for {@link SimpleLoadBalancer}
 *@author lepdou 2022-05-26
 */
public class SimpleLoadBalancerTest {

	@Test
	public void testSetterGetter() {
		List<Server> servers = new LinkedList<>();
		servers.add(Mockito.mock(Server.class));
		servers.add(Mockito.mock(Server.class));
		servers.add(Mockito.mock(Server.class));
		servers.add(Mockito.mock(Server.class));
		servers.add(Mockito.mock(Server.class));

		SimpleLoadBalancer simpleLoadBalancer = new SimpleLoadBalancer();

		simpleLoadBalancer.addServers(servers);

		List<Server> allServers = simpleLoadBalancer.getAllServers();
		List<Server> reachableServers = simpleLoadBalancer.getReachableServers();
		List<Server> availableServers = simpleLoadBalancer.getServerList(true);

		Assert.assertEquals(servers.size(), allServers.size());
		Assert.assertEquals(servers.size(), reachableServers.size());
		Assert.assertEquals(servers.size(), availableServers.size());
	}

	@Test
	public void testSetNull() {
		SimpleLoadBalancer simpleLoadBalancer = new SimpleLoadBalancer();

		simpleLoadBalancer.addServers(null);

		List<Server> allServers = simpleLoadBalancer.getAllServers();
		List<Server> reachableServers = simpleLoadBalancer.getReachableServers();
		List<Server> availableServers = simpleLoadBalancer.getServerList(true);

		Assert.assertEquals(0, allServers.size());
		Assert.assertEquals(0, reachableServers.size());
		Assert.assertEquals(0, availableServers.size());
	}
}
