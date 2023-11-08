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

package com.tencent.cloud.rpc.enhancement.config;

import java.util.Collections;
import java.util.List;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.feign.EnhancedFeignBeanPostProcessor;
import com.tencent.cloud.rpc.enhancement.feign.EnhancedLoadBalancerClientAspect;
import com.tencent.cloud.rpc.enhancement.filter.EnhancedReactiveFilter;
import com.tencent.cloud.rpc.enhancement.filter.EnhancedServletFilter;
import com.tencent.cloud.rpc.enhancement.plugin.DefaultEnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.reporter.ExceptionPolarisReporter;
import com.tencent.cloud.rpc.enhancement.plugin.reporter.SuccessPolarisReporter;
import com.tencent.cloud.rpc.enhancement.resttemplate.EnhancedRestTemplateInterceptor;
import com.tencent.cloud.rpc.enhancement.resttemplate.PolarisLoadBalancerRequestTransformer;
import com.tencent.cloud.rpc.enhancement.scg.EnhancedGatewayGlobalFilter;
import com.tencent.cloud.rpc.enhancement.transformer.InstanceTransformer;
import com.tencent.cloud.rpc.enhancement.transformer.PolarisInstanceTransformer;
import com.tencent.cloud.rpc.enhancement.webclient.EnhancedWebClientExchangeFilterFunction;
import com.tencent.cloud.rpc.enhancement.webclient.PolarisLoadBalancerClientRequestTransformer;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;

/**
 * Auto Configuration for Polaris {@link feign.Feign} OR {@link RestTemplate} which can automatically bring in the call
 * results for reporting.
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer.Xu</a> 2022-06-29
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
@ConditionalOnProperty(value = "spring.cloud.tencent.rpc-enhancement.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RpcEnhancementReporterProperties.class)
@AutoConfigureAfter(PolarisContextAutoConfiguration.class)
public class RpcEnhancementAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnMissingClass("com.alibaba.cloud.nacos.NacosServiceInstance")
	public InstanceTransformer instanceTransformer() {
		return new PolarisInstanceTransformer();
	}

	@Bean
	@Lazy
	public EnhancedPluginRunner enhancedFeignPluginRunner(
			@Autowired(required = false) List<EnhancedPlugin> enhancedPlugins,
			@Autowired(required = false) Registration registration,
			PolarisSDKContextManager polarisSDKContextManager) {
		return new DefaultEnhancedPluginRunner(enhancedPlugins, registration, polarisSDKContextManager.getSDKContext());
	}

	@Bean
	public SuccessPolarisReporter successPolarisReporter(RpcEnhancementReporterProperties properties,
			PolarisSDKContextManager polarisSDKContextManager) {
		return new SuccessPolarisReporter(properties, polarisSDKContextManager.getConsumerAPI());
	}

	@Bean
	public ExceptionPolarisReporter exceptionPolarisReporter(RpcEnhancementReporterProperties properties,
			PolarisSDKContextManager polarisSDKContextManager) {
		return new ExceptionPolarisReporter(properties, polarisSDKContextManager.getConsumerAPI());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	protected static class RpcEnhancementServletFilterConfig {

		@Bean
		public FilterRegistrationBean<EnhancedServletFilter> enhancedServletFilterRegistrationBean(
				EnhancedServletFilter enhancedServletFilter) {
			FilterRegistrationBean<EnhancedServletFilter> filterRegistrationBean =
					new FilterRegistrationBean<>(enhancedServletFilter);
			filterRegistrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE, REQUEST);
			filterRegistrationBean.setOrder(enhancedServletFilter.getClass().getAnnotation(Order.class).value());
			return filterRegistrationBean;
		}

		@Bean
		public EnhancedServletFilter enhancedServletFilter(@Lazy EnhancedPluginRunner pluginRunner) {
			return new EnhancedServletFilter(pluginRunner);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	protected static class RpcEnhancementReactiveFilterConfig {

		@Bean
		public EnhancedReactiveFilter enhancedReactiveFilter(@Lazy EnhancedPluginRunner pluginRunner) {
			return new EnhancedReactiveFilter(pluginRunner);
		}
	}

	/**
	 * Configuration for Polaris {@link feign.Feign} which can automatically bring in the call
	 * results for reporting.
	 *
	 * @author Haotian Zhang
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignAutoConfiguration")
	@AutoConfigureBefore(name = "org.springframework.cloud.openfeign.FeignAutoConfiguration")
	@Role(RootBeanDefinition.ROLE_INFRASTRUCTURE)
	protected static class PolarisFeignClientAutoConfiguration {

		@Bean
		public EnhancedFeignBeanPostProcessor polarisFeignBeanPostProcessor(@Lazy EnhancedPluginRunner pluginRunner) {
			return new EnhancedFeignBeanPostProcessor(pluginRunner);
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnClass(name = "org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser")
		public EnhancedLoadBalancerClientAspect enhancedLoadBalancerClientAspect() {
			return new EnhancedLoadBalancerClientAspect();
		}

	}

	/**
	 * Configuration for Polaris {@link RestTemplate} which can automatically bring in the call
	 * results for reporting.
	 *
	 * @author wh 2022/6/21
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
	protected static class PolarisRestTemplateAutoConfiguration {

		@Autowired(required = false)
		private List<RestTemplate> restTemplates = Collections.emptyList();

		@Bean
		public EnhancedRestTemplateInterceptor enhancedPolarisRestTemplateReporter(@Lazy EnhancedPluginRunner pluginRunner) {
			return new EnhancedRestTemplateInterceptor(pluginRunner);
		}

		@Bean
		public SmartInitializingSingleton setPolarisReporterForRestTemplate(EnhancedRestTemplateInterceptor reporter) {
			return () -> {
				for (RestTemplate restTemplate : restTemplates) {
					restTemplate.getInterceptors().add(reporter);
				}
			};
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnClass(name = {"org.springframework.cloud.client.loadbalancer.LoadBalancerRequestTransformer"})
		public PolarisLoadBalancerRequestTransformer polarisLoadBalancerRequestTransformer() {
			return new PolarisLoadBalancerRequestTransformer();
		}

	}

	/**
	 * Configuration for Polaris {@link org.springframework.web.reactive.function.client.WebClient} which can automatically bring in the call
	 * results for reporting.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
	protected static class PolarisWebClientAutoConfiguration {

		@Autowired(required = false)
		private List<WebClient.Builder> webClientBuilder = Collections.emptyList();

		@Bean
		public EnhancedWebClientExchangeFilterFunction exchangeFilterFunction(@Lazy EnhancedPluginRunner pluginRunner) {
			return new EnhancedWebClientExchangeFilterFunction(pluginRunner);
		}

		@Bean
		public SmartInitializingSingleton addEnhancedWebClientReporterForWebClient(EnhancedWebClientExchangeFilterFunction reporter) {
			return () -> webClientBuilder.forEach(webClient -> {
				webClient.filter(reporter);
			});
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnClass(name = "org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer")
		public PolarisLoadBalancerClientRequestTransformer polarisLoadBalancerClientRequestTransformer() {
			return new PolarisLoadBalancerClientRequestTransformer();
		}

	}

	/**
	 * Configuration for Polaris {@link org.springframework.web.reactive.function.client.WebClient} which can automatically bring in the call
	 * results for reporting.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.cloud.gateway.config.GatewayAutoConfiguration")
	@Role(RootBeanDefinition.ROLE_INFRASTRUCTURE)
	protected static class PolarisGatewayAutoConfiguration {

		@Bean
		@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
		public EnhancedGatewayGlobalFilter enhancedPolarisGatewayReporter(@Lazy EnhancedPluginRunner pluginRunner) {
			return new EnhancedGatewayGlobalFilter(pluginRunner);
		}
	}
}
