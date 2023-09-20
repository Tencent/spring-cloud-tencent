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

package com.tencent.cloud.polaris.registry;

import java.util.List;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.cloud.polaris.extend.consul.ConsulContextProperties;
import com.tencent.cloud.polaris.extend.nacos.NacosContextProperties;
import com.tencent.cloud.rpc.enhancement.stat.config.PolarisStatProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration of service registry of Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConditionalOnBean(AutoServiceRegistrationProperties.class)
@ConditionalOnPolarisRegisterEnabled
@AutoConfigureBefore(ServiceRegistryAutoConfiguration.class)
@AutoConfigureAfter({AutoServiceRegistrationAutoConfiguration.class, PolarisDiscoveryAutoConfiguration.class})
public class PolarisServiceRegistryAutoConfiguration {

	@Bean
	public PolarisServiceRegistry polarisServiceRegistry(
			PolarisDiscoveryProperties polarisDiscoveryProperties, PolarisSDKContextManager polarisSDKContextManager,
			PolarisDiscoveryHandler polarisDiscoveryHandler,
			StaticMetadataManager staticMetadataManager, PolarisStatProperties polarisStatProperties) {
		return new PolarisServiceRegistry(polarisDiscoveryProperties, polarisSDKContextManager, polarisDiscoveryHandler,
				staticMetadataManager, polarisStatProperties);
	}

	@Bean
	public PolarisRegistration polarisRegistration(
			PolarisDiscoveryProperties polarisDiscoveryProperties,
			PolarisContextProperties polarisContextProperties,
			@Autowired(required = false) ConsulContextProperties consulContextProperties,
			PolarisSDKContextManager polarisSDKContextManager, StaticMetadataManager staticMetadataManager,
			NacosContextProperties nacosContextProperties,
			@Autowired(required = false) ServletWebServerApplicationContext servletWebServerApplicationContext,
			@Autowired(required = false) ReactiveWebServerApplicationContext reactiveWebServerApplicationContext,
			@Autowired(required = false) List<PolarisRegistrationCustomizer> registrationCustomizers) {
		return PolarisRegistration.registration(polarisDiscoveryProperties, polarisContextProperties, consulContextProperties,
				polarisSDKContextManager.getSDKContext(), staticMetadataManager, nacosContextProperties,
				servletWebServerApplicationContext, reactiveWebServerApplicationContext, registrationCustomizers);
	}

	@Bean
	public PolarisAutoServiceRegistration polarisAutoServiceRegistration(
			PolarisServiceRegistry registry,
			AutoServiceRegistrationProperties autoServiceRegistrationProperties,
			PolarisRegistration registration,
			PolarisDiscoveryProperties polarisDiscoveryProperties,
			PolarisSDKContextManager polarisSDKContextManager
	) {
		return new PolarisAutoServiceRegistration(registry, autoServiceRegistrationProperties, registration,
				polarisDiscoveryProperties, polarisSDKContextManager.getAssemblyAPI());
	}

	@Bean
	public PolarisWebApplicationCheck polarisWebApplicationCheck() {
		return new PolarisWebApplicationCheck();
	}
}
