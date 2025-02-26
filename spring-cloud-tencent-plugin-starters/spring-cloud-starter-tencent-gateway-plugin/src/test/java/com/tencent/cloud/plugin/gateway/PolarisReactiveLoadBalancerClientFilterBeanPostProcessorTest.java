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

package com.tencent.cloud.plugin.gateway;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * Test for ${@link PolarisReactiveLoadBalancerClientFilterBeanPostProcessor}
 *
 * @author Shedfree Wu
 */
public class PolarisReactiveLoadBalancerClientFilterBeanPostProcessorTest {

	@Mock
	private ApplicationContext applicationContext;

	@Mock
	private LoadBalancerClientFactory loadBalancerClientFactory;

	@Mock
	private GatewayLoadBalancerProperties gatewayLoadBalancerProperties;

	private PolarisReactiveLoadBalancerClientFilterBeanPostProcessor processor;

	@BeforeEach
	protected void setUp() {
		MockitoAnnotations.openMocks(this);
		processor = new PolarisReactiveLoadBalancerClientFilterBeanPostProcessor(applicationContext);

		when(applicationContext.getBean(GatewayLoadBalancerProperties.class)).thenReturn(gatewayLoadBalancerProperties);
		when(applicationContext.getBean(LoadBalancerClientFactory.class)).thenReturn(loadBalancerClientFactory);
	}

	@Test
	void testGetOrder() {
		int order = processor.getOrder();
		Assertions.assertEquals(PolarisReactiveLoadBalancerClientFilterBeanPostProcessor.ORDER, order);
	}

	@Test
	void testPostProcessBeforeInitializationWithReactiveLoadBalancerClientFilter() {
		// Arrange
		ReactiveLoadBalancerClientFilter originalInterceptor = mock(ReactiveLoadBalancerClientFilter.class);
		String beanName = "testBean";

		// Act
		Object result = processor.postProcessAfterInitialization(originalInterceptor, beanName);

		// Assert
		Assertions.assertInstanceOf(PolarisReactiveLoadBalancerClientFilter.class, result);
	}

	@Test
	void testPostProcessBeforeInitializationWithNonReactiveLoadBalancerClientFilter() {
		// Arrange
		Object originalBean = new Object();
		String beanName = "testBean";

		// Act
		Object result = processor.postProcessAfterInitialization(originalBean, beanName);

		// Assert
		Assertions.assertSame(originalBean, result);
	}

}
