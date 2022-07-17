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

package com.tencent.cloud.plugin.gateway.staining;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link TrafficStainingGatewayFilter}.
 * @author lepdou 2022-07-12
 */
@RunWith(MockitoJUnitRunner.class)
public class TrafficStainerGatewayFilterTest {

	@Mock
	private GatewayFilterChain chain;
	@Mock
	private ServerWebExchange exchange;

	@Test
	public void testNoneTrafficStainingImplement() {
		TrafficStainingGatewayFilter filter = new TrafficStainingGatewayFilter(null);

		when(chain.filter(exchange)).thenReturn(Mono.empty());

		filter.filter(exchange, chain);

		verify(chain).filter(exchange);
	}

	@Test
	public void testMultiStaining() {
		TrafficStainer trafficStainer1 = Mockito.mock(TrafficStainer.class);
		TrafficStainer trafficStainer2 = Mockito.mock(TrafficStainer.class);

		when(trafficStainer1.getOrder()).thenReturn(1);
		when(trafficStainer2.getOrder()).thenReturn(2);

		Map<String, String> labels1 = new HashMap<>();
		labels1.put("k1", "v1");
		labels1.put("k2", "v2");
		when(trafficStainer1.apply(exchange)).thenReturn(labels1);

		Map<String, String> labels2 = new HashMap<>();
		labels2.put("k1", "v11");
		labels2.put("k3", "v3");
		when(trafficStainer2.apply(exchange)).thenReturn(labels2);

		TrafficStainingGatewayFilter filter = new TrafficStainingGatewayFilter(Arrays.asList(trafficStainer1, trafficStainer2));
		Map<String, String> result = filter.getStainedLabels(exchange);

		Assert.assertFalse(CollectionUtils.isEmpty(result));
		Assert.assertEquals("v1", result.get("k1"));
		Assert.assertEquals("v2", result.get("k2"));
		Assert.assertEquals("v3", result.get("k3"));
	}
}
