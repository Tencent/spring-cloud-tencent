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

package com.tencent.cloud.polaris.circuitbreaker.feign;

import com.tencent.polaris.api.core.ConsumerAPI;
import feign.Client;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PolarisFeignBeanPostProcessor}.
 *
 * @author Haotian Zhang
 */
public class PolarisFeignBeanPostProcessorTest {

	private PolarisFeignBeanPostProcessor polarisFeignBeanPostProcessor;

	@Before
	public void setUp() {
		ConsumerAPI consumerAPI = mock(ConsumerAPI.class);

		polarisFeignBeanPostProcessor = new PolarisFeignBeanPostProcessor(consumerAPI);
	}

	@Test
	public void testPostProcessBeforeInitialization() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		doAnswer(invocation -> {
			Class<?> clazz = invocation.getArgument(0);
			if (clazz.equals(BlockingLoadBalancerClient.class)) {
				return mock(BlockingLoadBalancerClient.class);
			}
			if (clazz.equals(CachingSpringLoadBalancerFactory.class)) {
				return mock(CachingSpringLoadBalancerFactory.class);
			}
			if (clazz.equals(SpringClientFactory.class)) {
				return mock(SpringClientFactory.class);
			}
			return null;
		}).when(beanFactory).getBean(any(Class.class));
		polarisFeignBeanPostProcessor.setBeanFactory(beanFactory);

		// isNeedWrap(bean) == false
		Object bean1 = new Object();
		Object bean = polarisFeignBeanPostProcessor.postProcessBeforeInitialization(bean1, "bean1");
		assertThat(bean).isNotInstanceOfAny(
				PolarisFeignClient.class,
				PolarisLoadBalancerFeignClient.class);

		// bean instanceOf Client.class
		Client bean2 = mock(Client.class);
		bean = polarisFeignBeanPostProcessor.postProcessBeforeInitialization(bean2, "bean2");
		assertThat(bean).isInstanceOf(PolarisFeignClient.class);

		// bean instanceOf LoadBalancerFeignClient.class
		LoadBalancerFeignClient bean3 = mock(LoadBalancerFeignClient.class);
		doReturn(mock(Client.class)).when(bean3).getDelegate();
		bean = polarisFeignBeanPostProcessor.postProcessBeforeInitialization(bean3, "bean3");
		assertThat(bean).isInstanceOf(PolarisLoadBalancerFeignClient.class);
	}
}
