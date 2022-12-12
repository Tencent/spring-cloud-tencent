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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.pojo.PolarisServiceInstance;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.CollectionUtils;

/**
 * load balancer utils.
 *
 * @author lepdou 2022-05-17
 */
public final class LoadBalancerUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerUtils.class);

	private static final int DEFAULT_WEIGHT = 100;
	private static final int WAIT_TIME = 3;

	private LoadBalancerUtils() {
	}

	/**
	 * transfer servers to ServiceInstances.
	 *
	 * @param servers servers
	 * @return ServiceInstances
	 */
	public static ServiceInstances transferServersToServiceInstances(Flux<List<ServiceInstance>> servers) {
		CountDownLatch latch = new CountDownLatch(1);

		AtomicReference<List<Instance>> instancesRef = new AtomicReference<>();
		servers.subscribe(serviceInstances -> {
			instancesRef.set(serviceInstances
					.stream()
					.map(LoadBalancerUtils::transferServerToServiceInstance)
					.collect(Collectors.toList()));

			latch.countDown();
		});

		try {
			latch.await(WAIT_TIME, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			LOGGER.error("Wait get instance result error. ", e);
		}

		String serviceName = "";
		if (!CollectionUtils.isEmpty(instancesRef.get())) {
			serviceName = instancesRef.get().get(0).getService();
		}

		ServiceKey serviceKey = new ServiceKey(MetadataContext.LOCAL_NAMESPACE, serviceName);
		return new DefaultServiceInstances(serviceKey, instancesRef.get());
	}

	/**
	 * transfer ServiceInstance to DefaultInstance.
	 *
	 * @param serviceInstance serviceInstance
	 * @return defaultInstance
	 */
	public static DefaultInstance transferServerToServiceInstance(ServiceInstance serviceInstance) {
		DefaultInstance instance = new DefaultInstance();
		instance.setNamespace(MetadataContext.LOCAL_NAMESPACE);
		instance.setService(serviceInstance.getServiceId());
		instance.setProtocol(serviceInstance.getScheme());
		instance.setId(serviceInstance.getInstanceId());
		instance.setHost(serviceInstance.getHost());
		instance.setPort(serviceInstance.getPort());
		instance.setWeight(DEFAULT_WEIGHT);
		instance.setMetadata(serviceInstance.getMetadata());

		if (serviceInstance instanceof PolarisServiceInstance) {
			PolarisServiceInstance polarisServiceInstance = (PolarisServiceInstance) serviceInstance;
			instance.setRegion(polarisServiceInstance.getPolarisInstance().getRegion());
			instance.setZone(polarisServiceInstance.getPolarisInstance().getZone());
			instance.setCampus(polarisServiceInstance.getPolarisInstance().getCampus());
		}

		return instance;
	}
}
