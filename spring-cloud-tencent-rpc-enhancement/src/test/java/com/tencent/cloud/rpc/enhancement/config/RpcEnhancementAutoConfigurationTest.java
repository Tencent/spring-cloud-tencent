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

package com.tencent.cloud.rpc.enhancement.config;

import java.util.concurrent.atomic.AtomicInteger;

import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.instrument.feign.EnhancedFeignBeanPostProcessor;
import com.tencent.cloud.rpc.enhancement.instrument.resttemplate.EnhancedRestTemplateInterceptor;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.reporter.ExceptionPolarisReporter;
import com.tencent.cloud.rpc.enhancement.plugin.reporter.SuccessPolarisReporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.loadbalancer.FeignLoadBalancerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Test For {@link RpcEnhancementAutoConfiguration}.
 *
 * @author Haotian Zhang, wh, Palmer Xu
 */
@DisplayName("RPC enhancement auto-configuration")
public class RpcEnhancementAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					RpcEnhancementPropertiesAutoConfiguration.class,
					RpcEnhancementAutoConfiguration.class,
					PolarisRestTemplateAutoConfigurationTester.class,
					FeignLoadBalancerAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.enabled=true", "spring.application.name=test", "spring.cloud.gateway.enabled=false");

	@Test
	public void testDefaultInitialization() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(EnhancedPluginRunner.class);
			assertThat(context).hasSingleBean(EnhancedFeignBeanPostProcessor.class);
			assertThat(context).hasSingleBean(SuccessPolarisReporter.class);
			assertThat(context).hasSingleBean(ExceptionPolarisReporter.class);
			assertThat(context).hasSingleBean(RestTemplate.class);
		});
	}

	/**
	 * Verify that RPC enhancement preserves advice on both regular and load-balanced templates.
	 */
	@DisplayName("RestTemplate retains AOP advice and the RPC enhancement interceptor")
	@Test
	void restTemplatesKeepAopAdviceAndRpcEnhancement() {
		new ApplicationContextRunner()
				.withUserConfiguration(RpcEnhancementAutoConfiguration.PolarisRestTemplateAutoConfiguration.class,
						RestTemplateAopConfiguration.class)
				.withBean(EnhancedPluginRunner.class, () -> mock(EnhancedPluginRunner.class))
				.run(context -> {
					assertThat(context).hasNotFailed();
					AtomicInteger adviceCalls = context.getBean(AtomicInteger.class);
					String url = "https://example.test/";
					for (String beanName : new String[] {"plainRestTemplate", "loadBalancedRestTemplate"}) {
						RestTemplate template = context.getBean(beanName, RestTemplate.class);
						assertThat(AopUtils.isAopProxy(template)).as(beanName).isTrue();
						assertThat(template.getInterceptors())
								.filteredOn(EnhancedRestTemplateInterceptor.class::isInstance)
								.hasSize(1);
						MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
						server.expect(requestTo(url)).andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));
						int before = adviceCalls.get();
						assertThat(template.getForObject(url, String.class)).isEqualTo("ok");
						assertThat(adviceCalls.get()).isEqualTo(before + 1);
						server.verify();
					}
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class RestTemplateAopConfiguration {

		@Bean
		RestTemplate plainRestTemplate() {
			return new RestTemplate();
		}

		@Bean
		@LoadBalanced
		RestTemplate loadBalancedRestTemplate() {
			return new RestTemplate();
		}

		@Bean
		AtomicInteger adviceCalls() {
			return new AtomicInteger();
		}

		@Bean
		Advisor restTemplateAdvisor(AtomicInteger adviceCalls) {
			NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
			pointcut.setClassFilter(RestTemplate.class::isAssignableFrom);
			pointcut.setMappedName("getForObject");
			MethodBeforeAdvice advice = (method, args, target) -> adviceCalls.incrementAndGet();
			return new DefaultPointcutAdvisor(pointcut, advice);
		}
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisRestTemplateAutoConfigurationTester {

		@Bean
		@LoadBalanced
		RestTemplate restTemplate() {
			return new RestTemplate();
		}
	}
}
