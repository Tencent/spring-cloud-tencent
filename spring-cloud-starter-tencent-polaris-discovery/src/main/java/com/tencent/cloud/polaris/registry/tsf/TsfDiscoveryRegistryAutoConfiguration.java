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
package com.tencent.cloud.polaris.registry.tsf;

import javax.servlet.ServletContext;

import com.tencent.cloud.common.tsf.ConditionalOnTsfConsulEnabled;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfCoreProperties;
import com.tencent.cloud.polaris.extend.consul.ConsulDiscoveryProperties;
import com.tencent.cloud.polaris.extend.consul.ConsulHeartbeatProperties;
import com.tencent.cloud.polaris.registry.PolarisServiceRegistryAutoConfiguration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration for TSF discovery.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnTsfConsulEnabled
@AutoConfigureBefore(PolarisServiceRegistryAutoConfiguration.class)
public class TsfDiscoveryRegistryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TsfPortPolarisRegistrationCustomizer tsfPortPolarisRegistrationCustomizer(
			AutoServiceRegistrationProperties autoServiceRegistrationProperties,
			ApplicationContext context, ConsulDiscoveryProperties consulDiscoveryProperties, TsfCoreProperties tsfCoreProperties,
			ConsulHeartbeatProperties consulHeartbeatProperties, PolarisSDKContextManager polarisSDKContextManager) {
		return new TsfPortPolarisRegistrationCustomizer(autoServiceRegistrationProperties, context,
				consulDiscoveryProperties, tsfCoreProperties, consulHeartbeatProperties, polarisSDKContextManager.getSDKContext());
	}

	@Bean
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	@ConditionalOnMissingBean
	public TsfServletRegistrationCustomizer tsfServletConsulCustomizer(ObjectProvider<ServletContext> servletContext) {
		return new TsfServletRegistrationCustomizer(servletContext);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(value = "tsf.swagger.enabled", havingValue = "true", matchIfMissing = true)
	public TsfApiPolarisRegistrationCustomizer tsfApiPolarisRegistrationCustomizer(ApplicationContext context) {
		return new TsfApiPolarisRegistrationCustomizer(context);
	}
}
