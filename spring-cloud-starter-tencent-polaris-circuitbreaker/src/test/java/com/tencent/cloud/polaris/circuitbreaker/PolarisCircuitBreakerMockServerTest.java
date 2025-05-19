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

package com.tencent.cloud.polaris.circuitbreaker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.protobuf.util.JsonFormat;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.circuitbreaker.config.PolarisCircuitBreakerProperties;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_CIRCUIT_BREAKER;
import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisCircuitBreaker} using real mock server.
 *
 * @author sean yu
 */
@ExtendWith(MockitoExtension.class)
public class PolarisCircuitBreakerMockServerTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	private static NamingServer namingServer;

	private static SDKContext context;

	@BeforeAll
	public static void beforeAll() throws IOException {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.namespace"))
				.thenReturn(NAMESPACE_TEST);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.service"))
				.thenReturn(SERVICE_CIRCUIT_BREAKER);
		PolarisSDKContextManager.innerDestroy();

		namingServer = NamingServer.startNamingServer(-1);
		System.setProperty(SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
		ServiceKey serviceKey = new ServiceKey(NAMESPACE_TEST, SERVICE_CIRCUIT_BREAKER);
		CircuitBreakerProto.CircuitBreakerRule.Builder circuitBreakerRuleBuilder = CircuitBreakerProto.CircuitBreakerRule.newBuilder();
		InputStream inputStream = PolarisCircuitBreakerMockServerTest.class.getClassLoader()
				.getResourceAsStream("circuitBreakerRule.json");
		String json = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining(""));
		JsonFormat.parser().ignoringUnknownFields().merge(json, circuitBreakerRuleBuilder);
		CircuitBreakerProto.CircuitBreakerRule circuitBreakerRule = circuitBreakerRuleBuilder.build();
		CircuitBreakerProto.CircuitBreaker circuitBreaker = CircuitBreakerProto.CircuitBreaker.newBuilder()
				.addRules(circuitBreakerRule).build();
		namingServer.getNamingService().setCircuitBreaker(serviceKey, circuitBreaker);

		Configuration configuration = TestUtils.configWithEnvAddress();
		context = SDKContext.initContextByConfig(configuration);
	}

	@AfterAll
	public static void afterAll() {
		if (null != namingServer) {
			namingServer.terminate();
		}
		if (null != mockedApplicationContextAwareUtils) {
			mockedApplicationContextAwareUtils.close();
		}
		if (null != context) {
			context.close();
		}
	}

	@Test
	public void testCircuitBreaker() {
		CircuitBreakAPI circuitBreakAPI = CircuitBreakAPIFactory.createCircuitBreakAPIByContext(context);
		ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(context);
		PolarisCircuitBreakerProperties polarisCircuitBreakerProperties = new PolarisCircuitBreakerProperties();
		PolarisCircuitBreakerFactory polarisCircuitBreakerFactory = new PolarisCircuitBreakerFactory(circuitBreakAPI, consumerAPI, polarisCircuitBreakerProperties);
		CircuitBreaker cb = polarisCircuitBreakerFactory.create(SERVICE_CIRCUIT_BREAKER);

		// trigger fallback for 5 times
		List<String> resList = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			int finalI = i;
			String res = cb.run(() -> {
				if (finalI % 2 == 1) {
					throw new IllegalArgumentException("invoke failed");
				}
				else {
					return "invoke success";
				}
			}, t -> "fallback");
			resList.add(res);
			Utils.sleepUninterrupted(2000);
		}
		assertThat(resList).isEqualTo(Arrays.asList("invoke success", "fallback", "fallback", "fallback", "fallback"));
	}

}
