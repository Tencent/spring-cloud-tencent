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
package com.tencent.cloud.polaris;

import com.tencent.cloud.common.util.inet.PolarisInetUtils;
import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.extend.consul.ConsulProperties;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfCoreProperties;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.cloud.polaris.extend.consul.ConsulDiscoveryConfigModifier;
import com.tencent.cloud.polaris.extend.consul.ConsulDiscoveryProperties;
import com.tencent.cloud.polaris.extend.consul.ConsulHeartbeatProperties;
import com.tencent.cloud.polaris.extend.nacos.NacosConfigModifier;
import com.tencent.cloud.polaris.extend.nacos.NacosContextProperties;
import io.micrometer.common.lang.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
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
	public PolarisDiscoveryConfigModifier polarisDiscoveryConfigModifier(PolarisDiscoveryProperties polarisDiscoveryProperties) {
		return new PolarisDiscoveryConfigModifier(polarisDiscoveryProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public NacosConfigModifier nacosConfigModifier(@Autowired(required = false) NacosContextProperties nacosContextProperties) {
		return new NacosConfigModifier(nacosContextProperties);
	}

	/**
	 * Create when consul is enabled.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "spring.cloud.consul.enabled", havingValue = "true")
	protected static class ConsulDiscoveryConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public ConsulDiscoveryProperties consulDiscoveryProperties(PolarisInetUtils polarisInetUtils) {
			return new ConsulDiscoveryProperties(polarisInetUtils);
		}

		@Bean
		@ConditionalOnMissingBean
		public ConsulHeartbeatProperties consulHeartbeatProperties() {
			return new ConsulHeartbeatProperties();
		}

		@Bean
		@ConditionalOnMissingBean
		public ConsulDiscoveryConfigModifier consulDiscoveryConfigModifier(
				PolarisDiscoveryProperties polarisDiscoveryProperties, ConsulProperties consulProperties,
				ConsulDiscoveryProperties consulContextProperties, ConsulHeartbeatProperties consulHeartbeatProperties,
				@Nullable TsfCoreProperties tsfCoreProperties, ApplicationContext applicationContext) {
			return new ConsulDiscoveryConfigModifier(polarisDiscoveryProperties, consulProperties, consulContextProperties,
					consulHeartbeatProperties, tsfCoreProperties, applicationContext);
		}
	}
}
