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
import com.tencent.cloud.rpc.enhancement.feign.DefaultEnhancedFeignPluginRunner;
import com.tencent.cloud.rpc.enhancement.feign.EnhancedFeignBeanPostProcessor;
import com.tencent.cloud.rpc.enhancement.feign.EnhancedFeignPluginRunner;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPlugin;
import com.tencent.cloud.rpc.enhancement.feign.plugin.reporter.ExceptionPolarisReporter;
import com.tencent.cloud.rpc.enhancement.feign.plugin.reporter.SuccessPolarisReporter;
import com.tencent.cloud.rpc.enhancement.resttemplate.BlockingLoadBalancerClientAspect;
import com.tencent.cloud.rpc.enhancement.resttemplate.EnhancedRestTemplateReporter;
import com.tencent.cloud.rpc.enhancement.webclient.EnhancedWebClientReporter;
import com.tencent.polaris.api.core.ConsumerAPI;

import com.tencent.polaris.client.api.SDKContext;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

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
		public EnhancedFeignPluginRunner enhancedFeignPluginRunner(
				@Autowired(required = false) List<EnhancedFeignPlugin> enhancedFeignPlugins) {
			return new DefaultEnhancedFeignPluginRunner(enhancedFeignPlugins);
		}

		@Bean
		public EnhancedFeignBeanPostProcessor polarisFeignBeanPostProcessor(@Lazy EnhancedFeignPluginRunner pluginRunner) {
			return new EnhancedFeignBeanPostProcessor(pluginRunner);
		}

		@Configuration
		static class PolarisReporterConfig {

			@Bean
			public SuccessPolarisReporter successPolarisReporter(RpcEnhancementReporterProperties properties,
																@Autowired(required = false) SDKContext context,
																@Autowired(required = false) ConsumerAPI consumerAPI) {
				return new SuccessPolarisReporter(properties, context, consumerAPI);
			}

			@Bean
			public ExceptionPolarisReporter exceptionPolarisReporter(RpcEnhancementReporterProperties properties,
																	 @Autowired(required = false) SDKContext context,
																	@Autowired(required = false) ConsumerAPI consumerAPI) {
				return new ExceptionPolarisReporter(properties, context, consumerAPI);
			}
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
		public EnhancedRestTemplateReporter enhancedRestTemplateReporter(
				RpcEnhancementReporterProperties properties, SDKContext context, ConsumerAPI consumerAPI) {
			return new EnhancedRestTemplateReporter(properties, context, consumerAPI);
		}

		@Bean
		public SmartInitializingSingleton setErrorHandlerForRestTemplate(EnhancedRestTemplateReporter reporter) {
			return () -> {
				for (RestTemplate restTemplate : restTemplates) {
					restTemplate.setErrorHandler(reporter);
				}
			};
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnClass(name = {"org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient"})
		public BlockingLoadBalancerClientAspect blockingLoadBalancerClientAspect() {
			return new BlockingLoadBalancerClientAspect();
		}
	}

	/**
	 * Configuration for Polaris {@link org.springframework.web.reactive.function.client.WebClient} which can automatically bring in the call
	 * results for reporting.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
	protected static class PolarisWebClientAutoConfiguration {

		@Bean
		public ExchangeFilterFunction exchangeFilterFunction(
				RpcEnhancementReporterProperties properties, SDKContext context, ConsumerAPI consumerAPI) {
			return new EnhancedWebClientReporter(properties, consumerAPI);
		}

	}
}
