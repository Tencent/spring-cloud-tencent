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

package com.tencent.cloud.polaris.eager.instrument.loadbalancer;


import java.util.List;

import com.tencent.polaris.api.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationListener;

/**
 * @author Yuwei Fu
 */
public class PolarisLoadBalancerEagerContextInitializer implements ApplicationListener<ApplicationReadyEvent> {


	private static final Logger LOG = LoggerFactory.getLogger(PolarisLoadBalancerEagerContextInitializer.class);

	private final LoadBalancerClientFactory factory;

	private final List<String> serviceNames;

	public PolarisLoadBalancerEagerContextInitializer(LoadBalancerClientFactory factory, List<String> serviceNames) {
		this.factory = factory;
		this.serviceNames = serviceNames;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {

		LOG.info("spring cloud eager-load start");
		try {
			if (!CollectionUtils.isEmpty(serviceNames)) {
				for (String serviceName : serviceNames) {
					LoadBalancerWarmUpUtils.warmUp(factory, serviceName);
				}
			}
			LOG.info("spring cloud eager-load end");
		}
		catch (Exception e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("spring cloud eager-load failed.", e);
			}
		}
	}
}
