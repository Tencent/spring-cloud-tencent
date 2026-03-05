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

package com.tencent.cloud.polaris.eager.instrument.feign;

import com.tencent.cloud.polaris.eager.instrument.loadbalancer.LoadBalancerEagerLoadProperties;
import com.tencent.cloud.polaris.registry.PolarisAutoServiceRegistration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

/**
 * Test for {@link FeignEagerLoadContextInitializer}.
 *
 * @author Test Author
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = DEFINED_PORT,
		classes = FeignEagerLoadContextInitializerTest.TestApplication.class,
		properties = {
				"server.port=48085",
				"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = servlet",
				"spring.cloud.gateway.enabled = false",
				"spring.cloud.polaris.discovery.eager-load.enabled = true",
				"spring.cloud.polaris.discovery.eager-load.feign.enabled = true",
				"spring.cloud.loadbalancer.eager-load.enabled = true",
				"spring.cloud.loadbalancer.eager-load.clients = test-service-1,test-service-2,test-feign-service-skip"
	})
public class FeignEagerLoadContextInitializerTest {

	@MockBean
	private LoadBalancerClientFactory loadBalancerClientFactory;

	@Autowired
	private FeignEagerLoadContextInitializer feignEagerLoadContextInitializer;

	@Autowired
	private LoadBalancerEagerLoadProperties loadBalancerEagerLoadProperties;

	@Autowired
	private ConfigurableApplicationContext applicationContext;

	@BeforeEach
	public void setUp() {
		reset(loadBalancerClientFactory);
	}

	@Test
	public void testLoadBalancerEagerLoadPropertiesLoaded() {
		// Verify LoadBalancerEagerLoadProperties is loaded correctly
		assertThat(loadBalancerEagerLoadProperties.getClients())
				.isNotNull()
				.containsExactlyInAnyOrder("test-service-1", "test-service-2", "test-feign-service-skip");
		assertThat(loadBalancerEagerLoadProperties.isEnabled()).isTrue();
	}

	@Test
	public void testFeignClient() {
		// Prepare mock
		ReactiveLoadBalancer<ServiceInstance> mockLoadBalancer = mock(ReactiveLoadBalancer.class);
		when(loadBalancerClientFactory.getInstance(anyString())).thenReturn(mockLoadBalancer);

		// Execute warm-up by triggering ApplicationReadyEvent
		feignEagerLoadContextInitializer.onApplicationEvent(mock(ApplicationReadyEvent.class));

		// Verify services in LoadBalancerEagerLoadProperties.clients are NOT warmed (skipped)
		// because they are handled by LoadBalancerEagerContextInitializer
		verify(loadBalancerClientFactory, never()).getInstance("test-service-1");
		verify(loadBalancerClientFactory, never()).getInstance("test-service-2");

		// Verify FeignClient services are warmed (without url)
		verify(loadBalancerClientFactory, times(1)).getInstance("test-feign-service");
		verify(loadBalancerClientFactory, times(1)).getInstance("test-feign-service-http");
		verify(loadBalancerClientFactory, times(1)).getInstance("test-feign-service-https");

		// Verify FeignClient with url is NOT warmed
		verify(loadBalancerClientFactory, never()).getInstance("localhost:48085");

		// Verify choose method is called
		verify(mockLoadBalancer, times(3)).choose();
	}

	@Test
	public void testSkipServicesInLoadBalancerEagerLoadProperties() {
		// Prepare mock
		ReactiveLoadBalancer<ServiceInstance> mockLoadBalancer = mock(ReactiveLoadBalancer.class);
		when(loadBalancerClientFactory.getInstance(anyString())).thenReturn(mockLoadBalancer);

		// Execute warm-up by triggering ApplicationReadyEvent
		feignEagerLoadContextInitializer.onApplicationEvent(mock(ApplicationReadyEvent.class));

		// Verify services configured in LoadBalancerEagerLoadProperties.clients are skipped
		// test-feign-service-skip is configured in LoadBalancerEagerLoadProperties.clients
		verify(loadBalancerClientFactory, never()).getInstance("test-feign-service-skip");
	}

	@Test
	public void testFeignClientWithHttpPrefix() {
		// Prepare mock
		ReactiveLoadBalancer<ServiceInstance> mockLoadBalancer = mock(ReactiveLoadBalancer.class);
		when(loadBalancerClientFactory.getInstance(anyString())).thenReturn(mockLoadBalancer);

		// Execute warm-up by triggering ApplicationReadyEvent
		feignEagerLoadContextInitializer.onApplicationEvent(mock(ApplicationReadyEvent.class));

		// Verify service name is correctly extracted from http:// prefix
		verify(loadBalancerClientFactory, times(1)).getInstance("test-feign-service-http");
	}

	@Test
	public void testFeignClientWithHttpsPrefix() {
		// Prepare mock
		ReactiveLoadBalancer<ServiceInstance> mockLoadBalancer = mock(ReactiveLoadBalancer.class);
		when(loadBalancerClientFactory.getInstance(anyString())).thenReturn(mockLoadBalancer);

		// Execute warm-up by triggering ApplicationReadyEvent
		feignEagerLoadContextInitializer.onApplicationEvent(mock(ApplicationReadyEvent.class));

		// Verify service name is correctly extracted from https:// prefix
		verify(loadBalancerClientFactory, times(1)).getInstance("test-feign-service-https");
	}

	@SpringBootApplication
	@EnableFeignClients
	@RestController
	protected static class TestApplication {

		@Bean
		public TestBeanPostProcessor testBeanPostProcessor() {
			return new TestBeanPostProcessor();
		}

		@Bean
		public FeignAspect feignAspect() {
			return new FeignAspect();
		}

		@RequestMapping("/test")
		public String test() {
			return "test";
		}

		// Normal FeignClient (without url)
		@FeignClient(name = "test-feign-service")
		public interface TestFeignClient {
			@RequestMapping("/test")
			String test();
		}

		// FeignClient with http:// prefix
		@FeignClient(name = "http://test-feign-service-http")
		public interface TestFeignClientWithHttp {
			@RequestMapping("/test")
			String test();
		}

		// FeignClient with https:// prefix
		@FeignClient(name = "https://test-feign-service-https")
		public interface TestFeignClientWithHttps {
			@RequestMapping("/test")
			String test();
		}

		// FeignClient with url (should skip warm-up)
		@FeignClient(name = "test-feign-with-url", url = "http://localhost:48085")
		public interface TestFeignClientWithUrl {
			@RequestMapping("/test")
			String test();
		}

		// FeignClient that is also in LoadBalancerEagerLoadProperties.clients
		// This service should be skipped in FeignEagerLoadContextInitializer
		@FeignClient(name = "test-feign-service-skip")
		public interface TestFeignClientSkip {
			@RequestMapping("/test")
			String test();
		}
	}

	static class TestBeanPostProcessor implements BeanPostProcessor {
		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof PolarisAutoServiceRegistration) {
				return org.mockito.Mockito.mock(PolarisAutoServiceRegistration.class);
			}
			return bean;
		}
	}

	@Aspect
	static class FeignAspect {

		private static final Logger LOG = LoggerFactory.getLogger(FeignAspect.class);

		@Around("@within(org.springframework.cloud.openfeign.FeignClient)")
		public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
			LOG.info("FeignAspect around");
			return joinPoint.proceed();
		}
	}
}
