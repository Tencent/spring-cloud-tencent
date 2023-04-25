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
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.feign.EnhancedFeignBeanPostProcessor;
import com.tencent.cloud.rpc.enhancement.feign.PolarisLoadBalancerFeignRequestTransformer;
import com.tencent.cloud.rpc.enhancement.filter.EnhancedReactiveFilter;
import com.tencent.cloud.rpc.enhancement.filter.EnhancedServletFilter;
import com.tencent.cloud.rpc.enhancement.plugin.DefaultEnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.assembly.client.AssemblyClientExceptionHook;
import com.tencent.cloud.rpc.enhancement.plugin.assembly.client.AssemblyClientPostHook;
import com.tencent.cloud.rpc.enhancement.plugin.assembly.client.AssemblyClientPreHook;
import com.tencent.cloud.rpc.enhancement.plugin.assembly.server.AssemblyServerExceptionHook;
import com.tencent.cloud.rpc.enhancement.plugin.assembly.server.AssemblyServerPostHook;
import com.tencent.cloud.rpc.enhancement.plugin.assembly.server.AssemblyServerPreHook;
import com.tencent.cloud.rpc.enhancement.plugin.reporter.ExceptionPolarisReporter;
import com.tencent.cloud.rpc.enhancement.plugin.reporter.SuccessPolarisReporter;
import com.tencent.cloud.rpc.enhancement.resttemplate.EnhancedRestTemplateInterceptor;
import com.tencent.cloud.rpc.enhancement.resttemplate.PolarisLoadBalancerRequestTransformer;
import com.tencent.cloud.rpc.enhancement.scg.EnhancedGatewayGlobalFilter;
import com.tencent.cloud.rpc.enhancement.webclient.EnhancedWebClientExchangeFilterFunction;
import com.tencent.cloud.rpc.enhancement.webclient.PolarisLoadBalancerClientRequestTransformer;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.client.api.SDKContext;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static jakarta.servlet.DispatcherType.ASYNC;
import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.FORWARD;
import static jakarta.servlet.DispatcherType.INCLUDE;
import static jakarta.servlet.DispatcherType.REQUEST;

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
	@Lazy
	public EnhancedPluginRunner enhancedFeignPluginRunner(
			@Autowired(required = false) List<EnhancedPlugin> enhancedPlugins,
			@Autowired(required = false) @Lazy Registration registration,
			SDKContext sdkContext) {
		return new DefaultEnhancedPluginRunner(enhancedPlugins, registration, sdkContext);
	}

	@Bean
	public SuccessPolarisReporter successPolarisReporter(RpcEnhancementReporterProperties properties,
			ConsumerAPI consumerAPI) {
		return new SuccessPolarisReporter(properties, consumerAPI);
	}

	@Bean
	public ExceptionPolarisReporter exceptionPolarisReporter(RpcEnhancementReporterProperties properties,
			ConsumerAPI consumerAPI) {
		return new ExceptionPolarisReporter(properties, consumerAPI);
	}

	@Bean
	public AssemblyClientExceptionHook assemblyClientExceptionHook(AssemblyAPI assemblyAPI) {
		return new AssemblyClientExceptionHook(assemblyAPI);
	}

	@Bean
	public AssemblyClientPostHook assemblyClientPostHook(AssemblyAPI assemblyAPI) {
		return new AssemblyClientPostHook(assemblyAPI);
	}

	@Bean
	public AssemblyClientPreHook assemblyClientPreHook(AssemblyAPI assemblyAPI) {
		return new AssemblyClientPreHook(assemblyAPI);
	}

	@Bean
	public AssemblyServerExceptionHook assemblyServerExceptionHook(AssemblyAPI assemblyAPI) {
		return new AssemblyServerExceptionHook(assemblyAPI);
	}

	@Bean
	public AssemblyServerPostHook assemblyServerPostHook(AssemblyAPI assemblyAPI) {
		return new AssemblyServerPostHook(assemblyAPI);
	}

	@Bean
	public AssemblyServerPreHook assemblyServerPreHook(AssemblyAPI assemblyAPI) {
		return new AssemblyServerPreHook(assemblyAPI);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	protected static class MetadataServletFilterConfig {

		@Bean
		public FilterRegistrationBean<EnhancedServletFilter> metadataServletFilterRegistrationBean(
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
	protected static class MetadataReactiveFilterConfig {

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
		@ConditionalOnClass(name = {"org.springframework.cloud.openfeign.loadbalancer.LoadBalancerFeignRequestTransformer"})
		public PolarisLoadBalancerFeignRequestTransformer polarisLoadBalancerFeignRequestTransformer() {
			return new PolarisLoadBalancerFeignRequestTransformer();
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

		@LoadBalanced
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
