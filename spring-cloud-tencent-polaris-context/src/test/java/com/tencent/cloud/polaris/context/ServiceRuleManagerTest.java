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

package com.tencent.cloud.polaris.context;

import java.util.List;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.rpc.ServiceRuleResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ServiceRuleManager}.
 *
 * @author wenxuan70
 */
@ExtendWith(MockitoExtension.class)
public class ServiceRuleManagerTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private SDKContext sdkContext;

	@Mock
	private ConsumerAPI consumerAPI;

	@BeforeEach
	public void setUp() {
		when(sdkContext.getConfig().getGlobal().getAPI().getTimeout()).thenReturn(500L);
	}

	@Test
	public void testGetServiceCircuitBreakerRule() {
		final String testNamespace = "testNamespace";
		final String testSourceService = "testSourceService";
		final String testDstService = "testDstService";

		CircuitBreakerProto.CircuitBreaker circuitBreaker = CircuitBreakerProto.CircuitBreaker.newBuilder()
				.addRules(CircuitBreakerProto.CircuitBreakerRule.newBuilder().build())
				.build();
		ServiceRuleByProto serviceRule = new ServiceRuleByProto(circuitBreaker,
				"111",
				false,
				ServiceEventKey.EventType.CIRCUIT_BREAKING);
		ServiceRuleResponse serviceRuleResponse = new ServiceRuleResponse(serviceRule);

		// source
		when(consumerAPI.getServiceRule(
				argThat(request -> request != null
						&& testNamespace.equals(request.getNamespace())
						&& testSourceService.equals(request.getService())
						&& ServiceEventKey.EventType.CIRCUIT_BREAKING.equals(request.getRuleType()))
		)).thenReturn(serviceRuleResponse);

		ServiceRuleResponse emptyRuleResponse = new ServiceRuleResponse(null);

		// destination
		when(consumerAPI.getServiceRule(
				argThat(request -> request != null
						&& testNamespace.equals(request.getNamespace())
						&& testDstService.equals(request.getService())
						&& ServiceEventKey.EventType.CIRCUIT_BREAKING.equals(request.getRuleType()))
		)).thenReturn(emptyRuleResponse);

		ServiceRuleManager serviceRuleManager = new ServiceRuleManager(sdkContext, consumerAPI);
		List<CircuitBreakerProto.CircuitBreakerRule> serviceCircuitBreakerRule = serviceRuleManager.getServiceCircuitBreakerRule(testNamespace,
				testSourceService,
				testDstService);

		assertThat(serviceCircuitBreakerRule).hasSize(1);
	}

	@Test
	public void testGetServiceRouterRule() {
		final String testNamespace = "testNamespace";
		final String testSourceService = "testSourceService";
		final String testDstService = "testDstService";

		RoutingProto.Routing routing = RoutingProto.Routing.newBuilder()
				.addOutbounds(RoutingProto.Route.newBuilder().build())
				.build();
		ServiceRule serviceRule = new ServiceRuleByProto(routing,
				"111",
				false,
				ServiceEventKey.EventType.ROUTING);
		ServiceRuleResponse serviceRuleResponse = new ServiceRuleResponse(serviceRule);

		// source
		when(consumerAPI.getServiceRule(
				argThat(request -> request != null
						&& testNamespace.equals(request.getNamespace())
						&& testSourceService.equals(request.getService())
						&& ServiceEventKey.EventType.ROUTING.equals(request.getRuleType()))
		)).thenReturn(serviceRuleResponse);


		ServiceRuleResponse emptyRuleResponse = new ServiceRuleResponse(null);

		// destination
		when(consumerAPI.getServiceRule(
				argThat(request -> request != null
						&& testNamespace.equals(request.getNamespace())
						&& testDstService.equals(request.getService())
						&& ServiceEventKey.EventType.ROUTING.equals(request.getRuleType()))
		)).thenReturn(emptyRuleResponse);

		ServiceRuleManager serviceRuleManager = new ServiceRuleManager(sdkContext, consumerAPI);
		List<RoutingProto.Route> serviceRouterRule = serviceRuleManager.getServiceRouterRule(testNamespace,
				testSourceService,
				testDstService);

		assertThat(serviceRouterRule).hasSize(1);
	}

	@Test
	public void testGetServiceRateLimitRule() {
		final String testNamespace = "testNamespace";
		final String testService = "testService";

		RateLimitProto.RateLimit rateLimit = RateLimitProto.RateLimit.getDefaultInstance();
		ServiceRule serviceRule = new ServiceRuleByProto(rateLimit,
				"111",
				false,
				ServiceEventKey.EventType.ROUTING);
		ServiceRuleResponse serviceRuleResponse = new ServiceRuleResponse(serviceRule);

		when(consumerAPI.getServiceRule(
				argThat(request -> request != null
						&& testNamespace.equals(request.getNamespace())
						&& testService.equals(request.getService())
						&& ServiceEventKey.EventType.RATE_LIMITING.equals(request.getRuleType()))
		)).thenReturn(serviceRuleResponse);

		ServiceRuleManager serviceRuleManager = new ServiceRuleManager(sdkContext, consumerAPI);
		RateLimitProto.RateLimit rateLimitRule = serviceRuleManager.getServiceRateLimitRule(testNamespace, testService);

		assertThat(rateLimitRule).isNotNull();
	}
}
