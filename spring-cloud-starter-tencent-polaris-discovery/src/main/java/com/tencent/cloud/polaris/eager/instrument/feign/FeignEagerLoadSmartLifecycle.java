/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.eager.instrument.feign;

import com.tencent.cloud.common.util.FeignUtil;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClient;
import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;

public class FeignEagerLoadSmartLifecycle implements SmartLifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(FeignEagerLoadSmartLifecycle.class);

	private final ApplicationContext applicationContext;

	private final PolarisDiscoveryClient polarisDiscoveryClient;

	private final PolarisReactiveDiscoveryClient polarisReactiveDiscoveryClient;

	public FeignEagerLoadSmartLifecycle(ApplicationContext applicationContext, PolarisDiscoveryClient polarisDiscoveryClient,
			PolarisReactiveDiscoveryClient polarisReactiveDiscoveryClient) {
		this.applicationContext = applicationContext;
		this.polarisDiscoveryClient = polarisDiscoveryClient;
		this.polarisReactiveDiscoveryClient = polarisReactiveDiscoveryClient;
	}

	@Override
	public void start() {
		LOG.info("feign eager-load start");
		for (String name : applicationContext.getBeanDefinitionNames()) {
			try {
				if (name.contains(FeignUtil.FEIGN_CLIENT_SPECIF) && !name.startsWith(FeignUtil.FEIGN_CLIENT_DEFAULT)) {
					String feignName = FeignUtil.analysisFeignName(name, applicationContext);
					if (StringUtils.isNotBlank(feignName)) {
						LOG.info("[{}] eager-load start", feignName);
						if (polarisDiscoveryClient != null) {
							polarisDiscoveryClient.getInstances(feignName);
						}
						else if (polarisReactiveDiscoveryClient != null) {
							polarisReactiveDiscoveryClient.getInstances(feignName).subscribe();
						}
						else {
							LOG.warn("[{}] no discovery client found.", feignName);
						}
						LOG.info("[{}] eager-load end", feignName);
					}
					else {
						LOG.warn("feign name is blank.");
					}
				}
			}
			catch (Exception e) {
				LOG.error("[{}] eager-load failed.", name, e);
			}
		}
		LOG.info("feign eager-load end");

	}

	@Override
	public void stop() {

	}

	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	public int getPhase() {
		return 10;
	}
}
