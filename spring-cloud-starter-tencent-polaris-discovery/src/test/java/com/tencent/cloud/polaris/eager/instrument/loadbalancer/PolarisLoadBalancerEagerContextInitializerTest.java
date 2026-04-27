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

package com.tencent.cloud.polaris.eager.instrument.loadbalancer;

import com.tencent.cloud.polaris.registry.PolarisAutoServiceRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

/**
 * Test for {@link PolarisLoadBalancerEagerContextInitializer}.
 *
 * @author Test Author
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = DEFINED_PORT,
		classes = PolarisLoadBalancerEagerContextInitializerTest.TestApplication.class,
		properties = {
				"server.port=48086",
				"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = servlet",
				"spring.cloud.gateway.server.webflux.enabled = false",
				"spring.cloud.polaris.discovery.eager-load.enabled = true",
				"spring.cloud.loadbalancer.eager-load.enabled = true",
				"spring.cloud.loadbalancer.eager-load.clients = test-service-1,test-service-2,test-service-3"
		})
public class PolarisLoadBalancerEagerContextInitializerTest {

	@MockBean
	private LoadBalancerClientFactory loadBalancerClientFactory;

	@Autowired
	private PolarisLoadBalancerEagerContextInitializer polarisLoadBalancerEagerContextInitializer;

	@Autowired
	private LoadBalancerEagerLoadProperties loadBalancerEagerLoadProperties;

	@BeforeEach
	public void setUp() {
		reset(loadBalancerClientFactory);
	}

	@Test
	public void testLoadBalancerEagerLoadPropertiesLoaded() {
		// Verify LoadBalancerEagerLoadProperties is loaded correctly
		assertThat(loadBalancerEagerLoadProperties.getClients())
				.isNotNull()
				.containsExactlyInAnyOrder("test-service-1", "test-service-2", "test-service-3");
		assertThat(loadBalancerEagerLoadProperties.isEnabled()).isTrue();
	}

	@Test
	public void testWarmUpServices() {
		// Prepare mock
		ReactiveLoadBalancer<ServiceInstance> mockLoadBalancer = mock(ReactiveLoadBalancer.class);
		when(loadBalancerClientFactory.getInstance(anyString())).thenReturn(mockLoadBalancer);

		// Execute warm-up by triggering ApplicationReadyEvent
		polarisLoadBalancerEagerContextInitializer.onApplicationEvent(mock(ApplicationReadyEvent.class));

		// Verify all services in LoadBalancerEagerLoadProperties.clients are warmed
		verify(loadBalancerClientFactory, times(1)).getInstance("test-service-1");
		verify(loadBalancerClientFactory, times(1)).getInstance("test-service-2");
		verify(loadBalancerClientFactory, times(1)).getInstance("test-service-3");

		// Verify choose method is called for each service
		verify(mockLoadBalancer, times(3)).choose();
	}

	@Test
	public void testWarmUpWithNullLoadBalancer() {
		// Prepare mock - return null for some services
		when(loadBalancerClientFactory.getInstance("test-service-1")).thenReturn(null);
		ReactiveLoadBalancer<ServiceInstance> mockLoadBalancer = mock(ReactiveLoadBalancer.class);
		when(loadBalancerClientFactory.getInstance("test-service-2")).thenReturn(mockLoadBalancer);
		when(loadBalancerClientFactory.getInstance("test-service-3")).thenReturn(mockLoadBalancer);

		// Execute warm-up by triggering ApplicationReadyEvent
		polarisLoadBalancerEagerContextInitializer.onApplicationEvent(mock(ApplicationReadyEvent.class));

		// Verify all services are attempted
		verify(loadBalancerClientFactory, times(1)).getInstance("test-service-1");
		verify(loadBalancerClientFactory, times(1)).getInstance("test-service-2");
		verify(loadBalancerClientFactory, times(1)).getInstance("test-service-3");

		// Verify choose method is only called for non-null load balancers
		verify(mockLoadBalancer, times(2)).choose();
	}

	@Test
	public void testWarmUpWithException() {
		// Prepare mock - throw exception for some services
		when(loadBalancerClientFactory.getInstance("test-service-1"))
				.thenThrow(new RuntimeException("Test exception"));
		ReactiveLoadBalancer<ServiceInstance> mockLoadBalancer = mock(ReactiveLoadBalancer.class);
		when(loadBalancerClientFactory.getInstance("test-service-2")).thenReturn(mockLoadBalancer);
		when(loadBalancerClientFactory.getInstance("test-service-3")).thenReturn(mockLoadBalancer);

		// Execute warm-up by triggering ApplicationReadyEvent
		polarisLoadBalancerEagerContextInitializer.onApplicationEvent(mock(ApplicationReadyEvent.class));

		// Verify all services are attempted (exception should not stop the loop)
		verify(loadBalancerClientFactory, times(1)).getInstance("test-service-1");
		verify(loadBalancerClientFactory, times(1)).getInstance("test-service-2");
		verify(loadBalancerClientFactory, times(1)).getInstance("test-service-3");
	}

	@SpringBootApplication
	protected static class TestApplication {

		@Bean
		public TestBeanPostProcessor testBeanPostProcessor() {
			return new TestBeanPostProcessor();
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
}
