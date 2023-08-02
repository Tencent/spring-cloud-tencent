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

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.cloud.polaris.extend.consul.ConsulConfigModifier;
import com.tencent.cloud.polaris.extend.consul.ConsulContextProperties;
import com.tencent.cloud.polaris.extend.nacos.NacosConfigModifier;
import com.tencent.cloud.polaris.extend.nacos.NacosContextProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Common configuration of discovery.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
public class DiscoveryPropertiesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PolarisDiscoveryProperties polarisDiscoveryProperties() {
		return new PolarisDiscoveryProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public ConsulContextProperties consulContextProperties() {
		return new ConsulContextProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public NacosContextProperties nacosContextProperties() {
		return new NacosContextProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public PolarisDiscoveryHandler polarisDiscoveryHandler(PolarisDiscoveryProperties polarisDiscoveryProperties,
			PolarisSDKContextManager polarisSDKContextManager) {
		return new PolarisDiscoveryHandler(polarisDiscoveryProperties, polarisSDKContextManager);
	}

	@Bean
	@ConditionalOnMissingBean
	public DiscoveryConfigModifier discoveryConfigModifier(PolarisDiscoveryProperties polarisDiscoveryProperties) {
		return new DiscoveryConfigModifier(polarisDiscoveryProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public ConsulConfigModifier consulConfigModifier(@Autowired(required = false) ConsulContextProperties consulContextProperties) {
		return new ConsulConfigModifier(consulContextProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public PolarisDiscoveryConfigModifier polarisDiscoveryConfigModifier(PolarisDiscoveryProperties polarisDiscoveryProperties) {
		return new PolarisDiscoveryConfigModifier(polarisDiscoveryProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public NacosConfigModifier nacosConfigModifier(@Autowired(required = false) NacosContextProperties nacosContextProperties) {
		return new NacosConfigModifier(nacosContextProperties);
	}
}
