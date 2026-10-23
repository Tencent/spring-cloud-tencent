/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.polaris.eager.instrument.services;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.SmartLifecycle;

public class ServicesEagerLoadSmartLifecycle implements SmartLifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(ServicesEagerLoadSmartLifecycle.class);

	private final PolarisDiscoveryClient polarisDiscoveryClient;

	private final PolarisReactiveDiscoveryClient polarisReactiveDiscoveryClient;

	private final AtomicBoolean running = new AtomicBoolean();

	public ServicesEagerLoadSmartLifecycle(PolarisDiscoveryClient polarisDiscoveryClient,
			PolarisReactiveDiscoveryClient polarisReactiveDiscoveryClient) {
		this.polarisDiscoveryClient = polarisDiscoveryClient;
		this.polarisReactiveDiscoveryClient = polarisReactiveDiscoveryClient;
	}

	@Override
	public void start() {
		if (!this.running.compareAndSet(false, true)) {
			return;
		}
		LOG.info("services eager-load start");
		try {
			if (polarisDiscoveryClient != null) {
				List<String> servicesList = polarisDiscoveryClient.getServices();
				LOG.info("eager-load got services: {}", servicesList);
				LOG.info("services eager-load end");
			}
			else if (polarisReactiveDiscoveryClient != null) {
				// getServices() is asynchronous, so the completion log is emitted
				// from the callbacks below rather than after subscribe().
				polarisReactiveDiscoveryClient.getServices()
						.collectList()
						.doOnSuccess(services -> {
							LOG.info("eager-load got services: {}", services);
							LOG.info("services eager-load end");
						})
						.doOnError(e -> {
							LOG.warn("services eager-load failed.", e);
							LOG.info("services eager-load end");
						})
						.subscribe();
			}
			else {
				LOG.warn("no discovery client found.");
			}
		}
		catch (Exception e) {
			LOG.warn("services eager-load failed.", e);
		}
	}

	@Override
	public void stop() {
		this.running.set(false);
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	public int getPhase() {
		return 10;
	}
}
