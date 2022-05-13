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
package com.tencent.cloud.polaris;

import javax.annotation.PostConstruct;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.cloud.polaris.extend.consul.ConsulContextProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Common configuration of discovery.
 *
 * @author Haotian Zhang
 */
@Configuration
@ConditionalOnPolarisEnabled
@Import({ PolarisDiscoveryProperties.class, ConsulContextProperties.class })
public class DiscoveryPropertiesAutoConfiguration {

	@Autowired(required = false)
	private PolarisDiscoveryProperties polarisDiscoveryProperties;

	@Autowired(required = false)
	private ConsulContextProperties consulContextProperties;

	private boolean registerEnabled = false;

	private boolean discoveryEnabled = false;

	@Bean(name = "polarisProvider")
	@ConditionalOnMissingBean
	public ProviderAPI polarisProvider(SDKContext polarisContext)
			throws PolarisException {
		return DiscoveryAPIFactory.createProviderAPIByContext(polarisContext);
	}

	@Bean(name = "polarisConsumer")
	@ConditionalOnMissingBean
	public ConsumerAPI polarisConsumer(SDKContext polarisContext)
			throws PolarisException {
		return DiscoveryAPIFactory.createConsumerAPIByContext(polarisContext);
	}

	@Bean
	@ConditionalOnMissingBean
	public PolarisDiscoveryHandler polarisDiscoveryHandler() {
		return new PolarisDiscoveryHandler();
	}

	@Bean
	public DiscoveryConfigModifier discoveryConfigModifier() {
		return new DiscoveryConfigModifier();
	}

	@PostConstruct
	public void init() {
		if (null != polarisDiscoveryProperties) {
			registerEnabled |= polarisDiscoveryProperties.isRegisterEnabled();
			discoveryEnabled |= polarisDiscoveryProperties.isEnabled();
		}
		if (null != consulContextProperties && consulContextProperties.isEnabled()) {
			registerEnabled |= consulContextProperties.isRegister();
			discoveryEnabled |= consulContextProperties.isDiscoveryEnabled();
		}
	}

	public boolean isRegisterEnabled() {
		return registerEnabled;
	}

	public boolean isDiscoveryEnabled() {
		return discoveryEnabled;
	}

}
