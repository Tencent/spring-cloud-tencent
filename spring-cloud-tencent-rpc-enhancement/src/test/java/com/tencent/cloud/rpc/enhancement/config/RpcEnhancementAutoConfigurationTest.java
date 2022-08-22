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

import java.io.IOException;
import java.net.URI;

import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.feign.EnhancedFeignBeanPostProcessor;
import com.tencent.cloud.rpc.enhancement.feign.EnhancedFeignPluginRunner;
import com.tencent.cloud.rpc.enhancement.feign.plugin.reporter.ExceptionPolarisReporter;
import com.tencent.cloud.rpc.enhancement.feign.plugin.reporter.SuccessPolarisReporter;
import com.tencent.cloud.rpc.enhancement.resttemplate.EnhancedRestTemplateReporter;
import com.tencent.polaris.api.core.ConsumerAPI;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.openfeign.loadbalancer.FeignLoadBalancerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test For {@link RpcEnhancementAutoConfiguration}.
 *
 * @author Haotian Zhang, wh, Palmer Xu
 */
public class RpcEnhancementAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					RpcEnhancementAutoConfiguration.class,
					PolarisRestTemplateAutoConfigurationTester.class,
					LoadBalancerAutoConfiguration.class,
					FeignLoadBalancerAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.enabled=true");

	@Test
	public void testDefaultInitialization() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ConsumerAPI.class);
			assertThat(context).hasSingleBean(EnhancedFeignPluginRunner.class);
			assertThat(context).hasSingleBean(EnhancedFeignBeanPostProcessor.class);
			assertThat(context).hasSingleBean(SuccessPolarisReporter.class);
			assertThat(context).hasSingleBean(ExceptionPolarisReporter.class);
			assertThat(context).hasSingleBean(EnhancedRestTemplateReporter.class);
			assertThat(context).hasSingleBean(RestTemplate.class);
			RestTemplate restTemplate = context.getBean(RestTemplate.class);
			assertThat(restTemplate.getErrorHandler() instanceof EnhancedRestTemplateReporter).isTrue();
		});
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisRestTemplateAutoConfigurationTester {

		@Bean
		@LoadBalanced
		RestTemplate restTemplate() {
			return new RestTemplate();
		}

		@Bean
		LoadBalancerClient loadBalancerClient() {
			return new LoadBalancerClient() {

				@Override
				public ServiceInstance choose(String serviceId) {
					return null;
				}

				@Override
				public <T> ServiceInstance choose(String serviceId, Request<T> request) {
					return null;
				}

				@Override
				public <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException {
					return null;
				}

				@Override
				public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException {
					return null;
				}

				@Override
				public URI reconstructURI(ServiceInstance instance, URI original) {
					return null;
				}
			};
		}
	}
}
