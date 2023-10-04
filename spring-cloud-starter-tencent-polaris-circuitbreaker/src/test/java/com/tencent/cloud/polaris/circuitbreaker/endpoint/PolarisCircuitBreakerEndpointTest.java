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

package com.tencent.cloud.polaris.circuitbreaker.endpoint;

import java.util.Map;

import com.google.protobuf.StringValue;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisCircuitBreakerEndpoint}.
 *
 * @author wenxuan70
 */
@ExtendWith(MockitoExtension.class)
public class PolarisCircuitBreakerEndpointTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(ApplicationContextAwareUtils.class)
			.withPropertyValues("spring.cloud.polaris.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.service=" + SERVICE_PROVIDER);

	private ServiceRuleManager serviceRuleManager;

	@BeforeEach
	void setUp() {
		serviceRuleManager = mock(ServiceRuleManager.class);
		when(serviceRuleManager.getServiceCircuitBreakerRule(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
			CircuitBreakerProto.CircuitBreakerRule.Builder ruleBuilder = CircuitBreakerProto.CircuitBreakerRule.newBuilder();
			ruleBuilder.setName("test_for_circuit_breaker");
			ruleBuilder.setEnable(true);
			ruleBuilder.setLevel(CircuitBreakerProto.Level.METHOD);
			CircuitBreakerProto.RuleMatcher.Builder rmBuilder = CircuitBreakerProto.RuleMatcher.newBuilder();
			rmBuilder.setDestination(CircuitBreakerProto.RuleMatcher.DestinationService.newBuilder().setNamespace("default").setService("svc2").setMethod(
					ModelProto.MatchString.newBuilder().setValue(StringValue.newBuilder().setValue("*").build()).build()).build());
			rmBuilder.setSource(CircuitBreakerProto.RuleMatcher.SourceService.newBuilder().setNamespace("*").setService("*").build());
			ruleBuilder.setRuleMatcher(rmBuilder.build());
			return CircuitBreakerProto.CircuitBreaker.newBuilder().addRules(ruleBuilder.build()).build().getRulesList();
		});
	}

	@Test
	public void testPolarisCircuitBreaker() {
		contextRunner.run(context -> {
			PolarisCircuitBreakerEndpoint endpoint = new PolarisCircuitBreakerEndpoint(serviceRuleManager);
			Map<String, Object> circuitBreakerInfo = endpoint.circuitBreaker("test");
			assertThat(circuitBreakerInfo).isNotNull();
			assertThat(circuitBreakerInfo.get("namespace")).isNotNull();
			assertThat(circuitBreakerInfo.get("service")).isNotNull();
			assertThat(circuitBreakerInfo.get("circuitBreakerRules")).asList().isNotEmpty();
		});
	}
}
