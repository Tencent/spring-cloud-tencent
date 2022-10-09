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
import java.util.List;

import com.netflix.loadbalancer.Server;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.pojo.PolarisServer;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;

/**
 * load balancer utils.
 *
 * @author lepdou 2022-05-17
 */
public final class LoadBalancerUtils {

	private LoadBalancerUtils() {
	}

	public static ServiceInstances transferServersToServiceInstances(List<Server> servers) {
		List<Instance> instances = new ArrayList<>(servers.size());
		String serviceName = null;

		for (Server server : servers) {
			if (server instanceof PolarisServer) {
				Instance instance = ((PolarisServer) server).getInstance();
				instances.add(instance);

				if (serviceName == null) {
					serviceName = instance.getService();
				}
			}
		}

		ServiceKey serviceKey = new ServiceKey(MetadataContext.LOCAL_NAMESPACE, serviceName);

		return new DefaultServiceInstances(serviceKey, instances);
	}
}
