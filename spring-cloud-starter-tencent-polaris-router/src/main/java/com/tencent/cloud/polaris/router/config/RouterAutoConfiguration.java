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

package com.tencent.cloud.polaris.router.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.beanprocessor.LoadBalancerClientFilterBeanPostProcessor;
import com.tencent.cloud.polaris.router.beanprocessor.LoadBalancerInterceptorBeanPostProcessor;
import com.tencent.cloud.polaris.router.beanprocessor.RetryLoadBalancerInterceptorBeanPostProcessor;
import com.tencent.cloud.polaris.router.config.properties.PolarisMetadataRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisNamespaceRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisNearByRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisRuleBasedRouterProperties;
import com.tencent.cloud.polaris.router.instrument.resttemplate.RouterContextFactory;
import com.tencent.cloud.polaris.router.instrument.resttemplate.RouterLabelRestTemplateInterceptor;
import com.tencent.cloud.polaris.router.instrument.scg.RouterLabelGlobalFilter;
import com.tencent.cloud.polaris.router.interceptor.MetadataRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.NamespaceRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.NearbyRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.RuleBasedRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.spi.ServletRouterLabelResolver;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;
import com.tencent.cloud.polaris.router.zuul.PolarisRibbonRoutingFilter;
import com.tencent.cloud.polaris.router.zuul.RouterLabelZuulFilter;
import com.tencent.cloud.rpc.enhancement.instrument.zuul.EnhancedZuulPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.zuul.ZuulServerAutoConfiguration;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * configuration for router module singleton beans.
 *
 * @author lepdou 2022-05-11
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisRouterEnabled
@RibbonClients(defaultConfiguration = {RibbonConfiguration.class})
public class RouterAutoConfiguration {

	@Bean
	@Order(HIGHEST_PRECEDENCE)
	@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.LoadBalancerClientFilter")
	public LoadBalancerClientFilterBeanPostProcessor loadBalancerClientFilterBeanPostProcessor() {
		return new LoadBalancerClientFilterBeanPostProcessor();
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

	@Bean
	@ConditionalOnProperty("spring.cloud.polaris.router.namespace-router.enabled")
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

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
	public static class RestTemplateAutoConfiguration {

		@Bean
		@Order(HIGHEST_PRECEDENCE)
		@ConditionalOnClass(name = "org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor")
		public LoadBalancerInterceptorBeanPostProcessor loadBalancerInterceptorBeanPostProcessor() {
			return new LoadBalancerInterceptorBeanPostProcessor();
		}

		@Bean
		@Order(HIGHEST_PRECEDENCE)
		@ConditionalOnClass(name = "org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor")
		public RetryLoadBalancerInterceptorBeanPostProcessor retryLoadBalancerInterceptorBeanPostProcessor() {
			return new RetryLoadBalancerInterceptorBeanPostProcessor();
		}

		@Bean
		@Order(HIGHEST_PRECEDENCE)
		public RouterContextFactory routerContextFactory(List<SpringWebRouterLabelResolver> routerLabelResolvers,
				StaticMetadataManager staticMetadataManager,
				RouterRuleLabelResolver routerRuleLabelResolver,
				PolarisContextProperties polarisContextProperties) {
			return new RouterContextFactory(routerLabelResolvers, staticMetadataManager,
					routerRuleLabelResolver, polarisContextProperties);
		}
	}

	/**
	 * AutoConfiguration for router module integrate for zuul.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "spring.cloud.polaris.router.zuul.enabled", matchIfMissing = true)
	@ConditionalOnClass(name = "org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter")
	@AutoConfigureAfter(ZuulServerAutoConfiguration.class)
	public static class ZuulRouterAutoConfiguration {

		@Bean(initMethod = "init")
		public PolarisRibbonRoutingFilter ribbonRoutingFilter(ProxyRequestHelper helper,
				RibbonCommandFactory<?> ribbonCommandFactory,
				StaticMetadataManager staticMetadataManager,
				RouterRuleLabelResolver routerRuleLabelResolver,
				List<ServletRouterLabelResolver> routerLabelResolvers,
				@Lazy EnhancedPluginRunner pluginRunner) {
			EnhancedZuulPluginRunner enhancedZuulPluginRunner = new EnhancedZuulPluginRunner(pluginRunner);

			return new PolarisRibbonRoutingFilter(helper, ribbonCommandFactory, staticMetadataManager,
					routerRuleLabelResolver, routerLabelResolvers, enhancedZuulPluginRunner);
		}

		@Bean
		public RouterLabelZuulFilter routerLabelZuulFilter() {
			return new RouterLabelZuulFilter();
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
