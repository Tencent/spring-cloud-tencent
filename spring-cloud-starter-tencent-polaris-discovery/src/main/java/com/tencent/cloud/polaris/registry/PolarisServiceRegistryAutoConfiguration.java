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

package com.tencent.cloud.polaris.registry;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.cloud.polaris.extend.consul.ConsulContextProperties;
import com.tencent.polaris.client.api.SDKContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration of service registry of Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConditionalOnPolarisRegisterEnabled
@ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
@AutoConfigureAfter({AutoServiceRegistrationConfiguration.class,
		AutoServiceRegistrationAutoConfiguration.class,
		PolarisDiscoveryAutoConfiguration.class})
public class PolarisServiceRegistryAutoConfiguration {

	@Bean
	public PolarisServiceRegistry polarisServiceRegistry(
			PolarisDiscoveryProperties polarisDiscoveryProperties, PolarisDiscoveryHandler polarisDiscoveryHandler,
			StaticMetadataManager staticMetadataManager) {
		return new PolarisServiceRegistry(polarisDiscoveryProperties, polarisDiscoveryHandler, staticMetadataManager);
	}

	@Bean
	@ConditionalOnBean(AutoServiceRegistrationProperties.class)
	public PolarisRegistration polarisRegistration(
			PolarisDiscoveryProperties polarisDiscoveryProperties,
			@Autowired(required = false) ConsulContextProperties consulContextProperties,
			SDKContext context, StaticMetadataManager staticMetadataManager) {
		return new PolarisRegistration(polarisDiscoveryProperties, consulContextProperties, context, staticMetadataManager);
	}

	@Bean
	@ConditionalOnBean(AutoServiceRegistrationProperties.class)
	public PolarisAutoServiceRegistration polarisAutoServiceRegistration(
			PolarisServiceRegistry registry,
			AutoServiceRegistrationProperties autoServiceRegistrationProperties,
			PolarisRegistration registration) {
		return new PolarisAutoServiceRegistration(registry, autoServiceRegistrationProperties, registration);
	}
}
