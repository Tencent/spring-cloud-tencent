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

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PolarisReactiveLoadBalancerClientFilter}.
 */
@ExtendWith(MockitoExtension.class)
class PolarisReactiveLoadBalancerClientFilterTest {

	@Mock
	private LoadBalancerClientFactory clientFactory;
	@Mock
	private GatewayLoadBalancerProperties properties;
	@Mock
	private ReactiveLoadBalancerClientFilter originalFilter;
	@Mock
	private ServerWebExchange exchange;
	@Mock
	private GatewayFilterChain chain;

	private PolarisReactiveLoadBalancerClientFilter polarisFilter;
	private final MetadataContext testContext = new MetadataContext();

	@BeforeEach
	void setUp() {
		polarisFilter = new PolarisReactiveLoadBalancerClientFilter(clientFactory, properties, originalFilter);
		MetadataContextHolder.remove();
	}

	@Test
	void testGetOrderDelegatesToOriginalFilter() {
		// Arrange
		when(originalFilter.getOrder()).thenReturn(42);

		// Act
		assertThat(polarisFilter.getOrder()).isEqualTo(42);
		verify(originalFilter).getOrder();
	}

	@Test
	void testFilterRestoresMetadataContext() {
		// Arrange
		when(exchange.getAttribute(MetadataConstant.HeaderName.METADATA_CONTEXT))
				.thenReturn(testContext);
		when(originalFilter.filter(exchange, chain))
				.thenReturn(Mono.empty());
		MetadataContext before = MetadataContextHolder.get();
		Assertions.assertNotEquals(testContext, before);
		// Act
		polarisFilter.filter(exchange, chain);
		MetadataContext after = MetadataContextHolder.get();
		Assertions.assertEquals(testContext, after);
	}

	@Test
	void testFilterWithoutMetadataContext() {
		// Arrange
		when(exchange.getAttribute(MetadataConstant.HeaderName.METADATA_CONTEXT))
				.thenReturn(null);
		when(originalFilter.filter(exchange, chain))
				.thenReturn(Mono.empty());
		MetadataContext before = MetadataContextHolder.get();
		// Act
		polarisFilter.filter(exchange, chain);
		MetadataContext after = MetadataContextHolder.get();

		// Assert
		Assertions.assertEquals(before, after);
	}
}
