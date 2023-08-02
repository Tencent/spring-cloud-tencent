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

import java.util.List;
import java.util.stream.Collectors;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.pojo.PolarisServiceInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

/**
 * Abstract Loadbalancer of Polaris.
 *
 * @author <a href="mailto:veteranchen@tencent.com">veteranchen</a>
 */
public abstract class AbstractPolarisLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private static final Logger log = LoggerFactory.getLogger(AbstractPolarisLoadBalancer.class);

	private final String serviceId;

	private final RouterAPI routerAPI;

	private ObjectProvider<ServiceInstanceListSupplier> supplierObjectProvider;

	public AbstractPolarisLoadBalancer(String serviceId, ObjectProvider<ServiceInstanceListSupplier> supplierObjectProvider, RouterAPI routerAPI) {
		this.serviceId = serviceId;
		this.supplierObjectProvider = supplierObjectProvider;
		this.routerAPI = routerAPI;
	}

	private static ServiceInstances convertToPolarisServiceInstances(List<ServiceInstance> serviceInstances) {
		ServiceKey serviceKey = new ServiceKey(MetadataContext.LOCAL_NAMESPACE, serviceInstances.get(0).getServiceId());
		List<Instance> polarisInstances = serviceInstances.stream()
				.map(serviceInstance -> ((PolarisServiceInstance) serviceInstance).getPolarisInstance())
				.collect(Collectors.toList());
		return new DefaultServiceInstances(serviceKey, polarisInstances);
	}

	@Override
	public Mono<Response<ServiceInstance>> choose(Request request) {
		ServiceInstanceListSupplier supplier = supplierObjectProvider
				.getIfAvailable(NoopServiceInstanceListSupplier::new);
		return supplier.get(request).next().map(serviceInstances -> {
			if (serviceInstances.isEmpty()) {
				log.warn("No servers available for service: " + this.serviceId);
				return new EmptyResponse();
			}

			ProcessLoadBalanceRequest req = new ProcessLoadBalanceRequest();
			req.setDstInstances(convertToPolarisServiceInstances(serviceInstances));
			req = setProcessLoadBalanceRequest(req);

			try {
				ProcessLoadBalanceResponse response = routerAPI.processLoadBalance(req);
				return new DefaultResponse(new PolarisServiceInstance(response.getTargetInstance()));
			}
			catch (Exception e) {
				log.warn("PolarisRoutingLoadbalancer error", e);
				return new EmptyResponse();
			}
		});
	}

	protected abstract ProcessLoadBalanceRequest setProcessLoadBalanceRequest(ProcessLoadBalanceRequest req);
}
