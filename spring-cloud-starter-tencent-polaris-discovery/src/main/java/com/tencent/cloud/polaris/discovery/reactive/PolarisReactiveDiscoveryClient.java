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

package com.tencent.cloud.polaris.discovery.reactive;

import java.util.function.Function;

import com.tencent.cloud.polaris.discovery.PolarisServiceDiscovery;
import com.tencent.polaris.api.exception.PolarisException;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

/**
 * Reactive Discovery Client for Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
public class PolarisReactiveDiscoveryClient implements ReactiveDiscoveryClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisReactiveDiscoveryClient.class);

	private final PolarisServiceDiscovery polarisServiceDiscovery;

	public PolarisReactiveDiscoveryClient(PolarisServiceDiscovery polarisServiceDiscovery) {
		this.polarisServiceDiscovery = polarisServiceDiscovery;
	}

	@Override
	public String description() {
		return "Spring Cloud Tencent Polaris Reactive Discovery Client";
	}

	@Override
	public Flux<ServiceInstance> getInstances(String serviceId) {

		return Mono.justOrEmpty(serviceId).flatMapMany(loadInstancesFromPolaris())
				.subscribeOn(Schedulers.boundedElastic());
	}

	private Function<String, Publisher<ServiceInstance>> loadInstancesFromPolaris() {
		return serviceId -> {
			try {
				return Flux.fromIterable(polarisServiceDiscovery.getInstances(serviceId));
			}
			catch (PolarisException e) {
				LOGGER.error("get service instance[{}] from polaris error!", serviceId, e);
				return Flux.empty();
			}
		};
	}

	@Override
	public Flux<String> getServices() {
		return Flux.defer(() -> {
			try {
				return Flux.fromIterable(polarisServiceDiscovery.getServices());
			}
			catch (Exception e) {
				LOGGER.error("get services from polaris server fail,", e);
				return Flux.empty();
			}
		}).subscribeOn(Schedulers.boundedElastic());
	}
}
