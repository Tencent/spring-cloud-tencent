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

package com.tencent.cloud.rpc.enhancement.feign;

import feign.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link EnhancedFeignBeanPostProcessor}.
 *
 * @author Haotian Zhang
 */
public class EnhancedFeignBeanPostProcessorTest {

	private EnhancedFeignBeanPostProcessor enhancedFeignBeanPostProcessor;

	@BeforeEach
	void setUp() {
		enhancedFeignBeanPostProcessor = new EnhancedFeignBeanPostProcessor(null);
	}

	@Test
	public void testPostProcessBeforeInitialization() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		doAnswer(invocation -> {
			Class<?> clazz = invocation.getArgument(0);
			if (clazz.equals(BlockingLoadBalancerClient.class)) {
				return mock(BlockingLoadBalancerClient.class);
			}
			if (clazz.equals(LoadBalancerProperties.class)) {
				return mock(LoadBalancerProperties.class);
			}
			if (clazz.equals(LoadBalancerClientFactory.class)) {
				return mock(LoadBalancerClientFactory.class);
			}
			return null;
		}).when(beanFactory).getBean(any(Class.class));
		enhancedFeignBeanPostProcessor.setBeanFactory(beanFactory);

		// isNeedWrap(bean) == false
		Object bean1 = new Object();
		Object bean = enhancedFeignBeanPostProcessor.postProcessBeforeInitialization(bean1, "bean1");
		assertThat(bean).isNotInstanceOfAny(EnhancedFeignClient.class);

		// bean instanceOf Client.class
		Client bean2 = mock(Client.class);
		bean = enhancedFeignBeanPostProcessor.postProcessBeforeInitialization(bean2, "bean2");
		assertThat(bean).isInstanceOf(EnhancedFeignClient.class);

		// bean instanceOf FeignBlockingLoadBalancerClient.class
		FeignBlockingLoadBalancerClient bean4 = mock(FeignBlockingLoadBalancerClient.class);
		doReturn(mock(Client.class)).when(bean4).getDelegate();
		bean = enhancedFeignBeanPostProcessor.postProcessBeforeInitialization(bean4, "bean4");
		assertThat(bean).isInstanceOf(FeignBlockingLoadBalancerClient.class);
		assertThat(((FeignBlockingLoadBalancerClient) bean).getDelegate()).isInstanceOf(EnhancedFeignClient.class);
	}
}
