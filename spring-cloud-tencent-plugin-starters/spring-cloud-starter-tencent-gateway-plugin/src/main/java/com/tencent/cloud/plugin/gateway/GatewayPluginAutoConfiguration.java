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

package com.tencent.cloud.plugin.gateway;

import com.tencent.cloud.plugin.gateway.context.ContextGatewayFilterFactory;
import com.tencent.cloud.plugin.gateway.context.ContextGatewayProperties;
import com.tencent.cloud.plugin.gateway.context.ContextGatewayPropertiesManager;
import com.tencent.cloud.plugin.gateway.context.ContextPropertiesRouteDefinitionLocator;
import com.tencent.cloud.plugin.gateway.context.ContextRoutePredicateFactory;
import com.tencent.cloud.plugin.gateway.context.GatewayConfigChangeListener;
import com.tencent.cloud.polaris.config.ConditionalOnPolarisConfigEnabled;
import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * Auto configuration for spring cloud gateway plugins.
 * @author lepdou 2022-07-06
 */
@Configuration
@ConditionalOnPolarisEnabled
@ConditionalOnProperty(value = "spring.cloud.tencent.plugin.scg.enabled", matchIfMissing = true)
public class GatewayPluginAutoConfiguration {

	@Configuration
	@ConditionalOnProperty(value = "spring.cloud.tencent.plugin.scg.context.enabled", matchIfMissing = true)
	@ConditionalOnPolarisConfigEnabled
	@ConditionalOnClass(GlobalFilter.class)
	@Import(ContextGatewayProperties.class)
	public static class ContextPluginConfiguration {

		@Value("${spring.cloud.polaris.discovery.eager-load.enabled:#{'true'}}")
		private boolean commonEagerLoadEnabled;

		@Value("${spring.cloud.polaris.discovery.eager-load.gateway.enabled:#{'true'}}")
		private boolean gatewayEagerLoadEnabled;

		@Bean
		public ContextGatewayFilterFactory contextGatewayFilterFactory(ContextGatewayPropertiesManager contextGatewayPropertiesManager) {
			return new ContextGatewayFilterFactory(contextGatewayPropertiesManager);
		}

		@Bean
		public ContextPropertiesRouteDefinitionLocator contextPropertiesRouteDefinitionLocator(ContextGatewayProperties properties) {
			return new ContextPropertiesRouteDefinitionLocator(properties);
		}

		@Bean
		public ContextRoutePredicateFactory contextServiceRoutePredicateFactory() {
			return new ContextRoutePredicateFactory();
		}

		@Bean
		public ContextGatewayPropertiesManager contextGatewayPropertiesManager(ContextGatewayProperties properties,
				@Autowired(required = false) PolarisDiscoveryClient polarisDiscoveryClient,
				@Autowired(required = false) PolarisReactiveDiscoveryClient polarisReactiveDiscoveryClient) {
			ContextGatewayPropertiesManager contextGatewayPropertiesManager = new ContextGatewayPropertiesManager();
			contextGatewayPropertiesManager.setGroupRouteMap(properties.getGroups());
			if (commonEagerLoadEnabled && gatewayEagerLoadEnabled) {
				contextGatewayPropertiesManager.eagerLoad(polarisDiscoveryClient, polarisReactiveDiscoveryClient);
			}
			return contextGatewayPropertiesManager;
		}

		@Bean
		public GatewayRegistrationCustomizer gatewayRegistrationCustomizer() {
			return new GatewayRegistrationCustomizer();
		}

		@Bean
		public GatewayConfigChangeListener gatewayConfigChangeListener(ContextGatewayPropertiesManager manager,
				ApplicationEventPublisher publisher, Environment environment)  {
			return new GatewayConfigChangeListener(manager, publisher, environment);
		}

		@Bean
		public PolarisReactiveLoadBalancerClientFilterBeanPostProcessor polarisReactiveLoadBalancerClientFilterBeanPostProcessor(
				ApplicationContext applicationContext) {
			return new PolarisReactiveLoadBalancerClientFilterBeanPostProcessor(applicationContext);
		}
	}
}
