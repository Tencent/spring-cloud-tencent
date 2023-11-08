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

package com.tencent.cloud.polaris.router.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.beanprocessor.ReactiveLoadBalancerClientFilterBeanPostProcessor;
import com.tencent.cloud.polaris.router.config.properties.PolarisMetadataRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisNearByRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisRuleBasedRouterProperties;
import com.tencent.cloud.polaris.router.interceptor.MetadataRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.NearbyRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.RuleBasedRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.resttemplate.RouterLabelRestTemplateInterceptor;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;
import com.tencent.cloud.rpc.enhancement.resttemplate.EnhancedRestTemplateInterceptor;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * configuration for router module singleton beans.
 *
 *@author lepdou 2022-05-11
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisRouterEnabled
@LoadBalancerClients(defaultConfiguration = LoadBalancerConfiguration.class)
@Import({PolarisNearByRouterProperties.class, PolarisMetadataRouterProperties.class, PolarisRuleBasedRouterProperties.class})
public class RouterAutoConfiguration {

	@Bean
	@Order(HIGHEST_PRECEDENCE)
	@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter")
	public ReactiveLoadBalancerClientFilterBeanPostProcessor loadBalancerClientFilterBeanPostProcessor() {
		return new ReactiveLoadBalancerClientFilterBeanPostProcessor();
	}

	@Bean
	public RouterRuleLabelResolver routerRuleLabelResolver(ServiceRuleManager serviceRuleManager) {
		return new RouterRuleLabelResolver(serviceRuleManager);
	}

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
		public RouterLabelRestTemplateInterceptor routerLabelRestTemplateInterceptor(
				List<SpringWebRouterLabelResolver> routerLabelResolvers,
				StaticMetadataManager staticMetadataManager,
				RouterRuleLabelResolver routerRuleLabelResolver,
				PolarisContextProperties polarisContextProperties) {
			return new RouterLabelRestTemplateInterceptor(routerLabelResolvers, staticMetadataManager,
					routerRuleLabelResolver, polarisContextProperties);
		}

		@Bean
		public SmartInitializingSingleton addRouterLabelInterceptorForRestTemplate(RouterLabelRestTemplateInterceptor interceptor) {
			return () -> restTemplates.forEach(restTemplate -> {
				List<ClientHttpRequestInterceptor> list = new ArrayList<>(restTemplate.getInterceptors());
				int addIndex = list.size();
				for (int i = 0; i < list.size(); i++) {
					if (list.get(i) instanceof EnhancedRestTemplateInterceptor) {
						addIndex = i;
					}
				}
				list.add(addIndex, interceptor);
				restTemplate.setInterceptors(list);
			});
		}
	}
}
