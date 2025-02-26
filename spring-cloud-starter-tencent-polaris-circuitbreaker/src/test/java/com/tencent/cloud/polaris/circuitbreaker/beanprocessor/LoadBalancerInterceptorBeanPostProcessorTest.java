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

package com.tencent.cloud.polaris.circuitbreaker.beanprocessor;

import com.tencent.cloud.polaris.circuitbreaker.instrument.resttemplate.PolarisLoadBalancerInterceptor;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for ${@link LoadBalancerInterceptorBeanPostProcessor}.
 *
 * @author Shedfree Wu
 */
class LoadBalancerInterceptorBeanPostProcessorTest {

	@Mock
	private BeanFactory beanFactory;

	@Mock
	private LoadBalancerRequestFactory requestFactory;

	@Mock
	private LoadBalancerClient loadBalancerClient;

	@Mock
	private EnhancedPluginRunner pluginRunner;

	private LoadBalancerInterceptorBeanPostProcessor processor;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		processor = new LoadBalancerInterceptorBeanPostProcessor();
		processor.setBeanFactory(beanFactory);

		// Setup mock behavior
		when(beanFactory.getBean(LoadBalancerRequestFactory.class)).thenReturn(requestFactory);
		when(beanFactory.getBean(LoadBalancerClient.class)).thenReturn(loadBalancerClient);
		when(beanFactory.getBean(EnhancedPluginRunner.class)).thenReturn(pluginRunner);
	}

	@Test
	void testPostProcessBeforeInitializationWithLoadBalancerInterceptor() {
		// Arrange
		LoadBalancerInterceptor originalInterceptor = mock(LoadBalancerInterceptor.class);
		String beanName = "testBean";

		// Act
		Object result = processor.postProcessBeforeInitialization(originalInterceptor, beanName);

		// Assert
		Assertions.assertInstanceOf(PolarisLoadBalancerInterceptor.class, result);
		verify(beanFactory).getBean(LoadBalancerRequestFactory.class);
		verify(beanFactory).getBean(LoadBalancerClient.class);
		verify(beanFactory).getBean(EnhancedPluginRunner.class);
	}

	@Test
	void testPostProcessBeforeInitializationWithNonLoadBalancerInterceptor() {
		// Arrange
		Object originalBean = new Object();
		String beanName = "testBean";

		// Act
		Object result = processor.postProcessBeforeInitialization(originalBean, beanName);

		// Assert
		Assertions.assertSame(originalBean, result);
	}

	@Test
	void testGetOrder() {
		// Act
		int order = processor.getOrder();

		// Assert
		Assertions.assertEquals(LoadBalancerInterceptorBeanPostProcessor.POLARIS_LOAD_BALANCER_INTERCEPTOR_POST_PROCESSOR_ORDER, order);
	}

	@Test
	void testSetBeanFactory() {
		// Arrange
		BeanFactory newBeanFactory = mock(BeanFactory.class);
		LoadBalancerInterceptorBeanPostProcessor newProcessor = new LoadBalancerInterceptorBeanPostProcessor();

		// Act
		newProcessor.setBeanFactory(newBeanFactory);

		// Assert
		// Verify the bean factory is set by trying to process a bean
		LoadBalancerInterceptor interceptor = mock(LoadBalancerInterceptor.class);
		when(newBeanFactory.getBean(LoadBalancerRequestFactory.class)).thenReturn(requestFactory);
		when(newBeanFactory.getBean(LoadBalancerClient.class)).thenReturn(loadBalancerClient);
		when(newBeanFactory.getBean(EnhancedPluginRunner.class)).thenReturn(pluginRunner);

		Object result = newProcessor.postProcessBeforeInitialization(interceptor, "testBean");
		Assertions.assertInstanceOf(PolarisLoadBalancerInterceptor.class, result);
	}
}
