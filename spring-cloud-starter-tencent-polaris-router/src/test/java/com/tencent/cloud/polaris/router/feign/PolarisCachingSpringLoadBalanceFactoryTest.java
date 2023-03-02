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
 *
 */

package com.tencent.cloud.polaris.router.feign;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.ILoadBalancer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.ribbon.FeignLoadBalancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisCachingSpringLoadBalanceFactory}.
 *
 * @author lepdou 2022-05-26
 */
@ExtendWith(MockitoExtension.class)
public class PolarisCachingSpringLoadBalanceFactoryTest {

	private final String service1 = "service1";
	private final String service2 = "service2";
	@Mock
	private SpringClientFactory factory;

	@Test
	public void test() {
		PolarisCachingSpringLoadBalanceFactory polarisCachingSpringLoadBalanceFactory =
				new PolarisCachingSpringLoadBalanceFactory(factory, null);

		DefaultClientConfigImpl config1 = new DefaultClientConfigImpl();
		config1.loadDefaultValues();
		config1.setClientName(service1);
		DefaultClientConfigImpl config2 = new DefaultClientConfigImpl();
		config2.loadDefaultValues();
		config2.setClientName(service2);

		when(factory.getClientConfig(service1)).thenReturn(config1);
		when(factory.getClientConfig(service2)).thenReturn(config2);

		ILoadBalancer loadBalancer = new SimpleLoadBalancer();
		when(factory.getLoadBalancer(service1)).thenReturn(loadBalancer);
		when(factory.getLoadBalancer(service2)).thenReturn(loadBalancer);

		ServerIntrospector serverIntrospector = new DefaultServerIntrospector();
		when(factory.getInstance(service1, ServerIntrospector.class)).thenReturn(serverIntrospector);
		when(factory.getInstance(service2, ServerIntrospector.class)).thenReturn(serverIntrospector);

		// load balancer for service1
		FeignLoadBalancer feignLoadBalancer = polarisCachingSpringLoadBalanceFactory.create(service1);

		assertThat(feignLoadBalancer).isNotNull();
		verify(factory).getClientConfig(service1);
		verify(factory, times(0)).getClientConfig(service2);
		verify(factory).getLoadBalancer(service1);
		verify(factory, times(0)).getLoadBalancer(service2);
		verify(factory).getInstance(service1, ServerIntrospector.class);
		verify(factory, times(0)).getInstance(service2, ServerIntrospector.class);
		assertThat(feignLoadBalancer.getLoadBalancer()).isEqualTo(loadBalancer);
		assertThat(feignLoadBalancer.getClientName()).isEqualTo(service1);

		// load balancer for service2
		FeignLoadBalancer feignLoadBalancer2 = polarisCachingSpringLoadBalanceFactory.create(service2);
		// load balancer for service1 again
		feignLoadBalancer = polarisCachingSpringLoadBalanceFactory.create(service1);

		assertThat(feignLoadBalancer2).isNotNull();
		verify(factory).getClientConfig(service1);
		verify(factory).getClientConfig(service2);
		verify(factory).getLoadBalancer(service1);
		verify(factory).getLoadBalancer(service2);
		verify(factory).getInstance(service1, ServerIntrospector.class);
		verify(factory).getInstance(service2, ServerIntrospector.class);
		assertThat(feignLoadBalancer2.getLoadBalancer()).isEqualTo(loadBalancer);
		assertThat(feignLoadBalancer2.getClientName()).isEqualTo(service2);

	}
}
