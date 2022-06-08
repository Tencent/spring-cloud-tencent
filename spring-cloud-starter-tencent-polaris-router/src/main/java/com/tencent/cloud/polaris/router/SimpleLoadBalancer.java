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

import java.util.Collections;
import java.util.List;

import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

/**
 * Simple load balancer only for getting and setting servers.
 *
 *@author lepdou 2022-05-17
 */
public class SimpleLoadBalancer implements ILoadBalancer {
	private List<Server> servers;

	@Override
	public void addServers(List<Server> newServers) {
		this.servers = newServers;
	}

	@Override
	public Server chooseServer(Object key) {
		return null;
	}

	@Override
	public void markServerDown(Server server) {

	}

	@Override
	public List<Server> getServerList(boolean availableOnly) {
		if (servers == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(servers);
	}

	@Override
	public List<Server> getReachableServers() {
		if (servers == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(servers);
	}

	@Override
	public List<Server> getAllServers() {
		if (servers == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(servers);
	}
}
