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

package com.tencent.cloud.polaris.router.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tencent.cloud.polaris.router.config.properties.PolarisMetadataRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisNamespaceRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisNearByRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisRuleBasedRouterProperties;
import com.tencent.cloud.polaris.router.instrument.resttemplate.RouterLabelRestTemplateInterceptor;
import com.tencent.cloud.polaris.router.instrument.scg.RouterLabelGlobalFilter;
import com.tencent.cloud.polaris.router.interceptor.MetadataRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.NamespaceRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.NearbyRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.RuleBasedRouterRequestInterceptor;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

/**
 * configuration for router module singleton beans.
 *
 *@author lepdou 2022-05-11
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisRouterEnabled
@LoadBalancerClients(defaultConfiguration = LoadBalancerConfiguration.class)
public class RouterAutoConfiguration {

	@Bean
	@ConditionalOnProperty(value = "spring.cloud.polaris.router.metadata-router.enabled", matchIfMissing = true)
	public MetadataRouterRequestInterceptor metadataRouterRequestInterceptor(PolarisMetadataRouterProperties polarisMetadataRouterProperties) {
		return new MetadataRouterRequestInterceptor(polarisMetadataRouterProperties);
	}

	@Bean
	@ConditionalOnProperty(value = "spring.cloud.polaris.router.nearby-router.enabled", matchIfMissing = true)
	public NearbyRouterRequestInterceptor nearbyRouterRequestInterceptor(PolarisNearByRouterProperties polarisNearByRouterProperties) {
		return new NearbyRouterRequestInterceptor(polarisNearByRouterProperties);
	}

	@Bean
	@ConditionalOnProperty(value = "spring.cloud.polaris.router.rule-router.enabled", matchIfMissing = true)
	public RuleBasedRouterRequestInterceptor ruleBasedRouterRequestInterceptor(PolarisRuleBasedRouterProperties polarisRuleBasedRouterProperties) {
		return new RuleBasedRouterRequestInterceptor(polarisRuleBasedRouterProperties);
	}

	@Bean
	@ConditionalOnProperty(value = "spring.cloud.polaris.router.namespace-router.enabled", matchIfMissing = true)
	public NamespaceRouterRequestInterceptor namespaceRouterRequestInterceptor(PolarisNamespaceRouterProperties polarisNamespaceRouterProperties) {
		return new NamespaceRouterRequestInterceptor(polarisNamespaceRouterProperties);
	}

	/**
	 * Create when gateway application is SCG.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
	protected static class RouterLabelScgFilterConfig {

		@Bean
		public RouterLabelGlobalFilter routerLabelGlobalFilter() {
			return new RouterLabelGlobalFilter();
		}

	}

	/**
	 * Create when RestTemplate exists.
	 * @author liuye 2022-09-14
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
	@ConditionalOnProperty(value = "spring.cloud.polaris.router.rule-router.enabled", matchIfMissing = true)
	protected static class RouterLabelRestTemplateConfig {

		@Autowired(required = false)
		private List<RestTemplate> restTemplates = Collections.emptyList();

		@Bean
		public RouterLabelRestTemplateInterceptor routerLabelRestTemplateInterceptor() {
			return new RouterLabelRestTemplateInterceptor();
		}

		@Bean
		public SmartInitializingSingleton addRouterLabelInterceptorForRestTemplate(RouterLabelRestTemplateInterceptor interceptor) {
			return () -> restTemplates.forEach(restTemplate -> {
				List<ClientHttpRequestInterceptor> list = new ArrayList<>(restTemplate.getInterceptors());
				list.add(interceptor);
				restTemplate.setInterceptors(list);
			});
		}
	}
}
