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

package com.tencent.cloud.polaris.circuitbreaker.config;


import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.circuitbreaker.PolarisCircuitBreaker;
import com.tencent.cloud.polaris.circuitbreaker.PolarisCircuitBreakerFactory;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementAutoConfiguration;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_CIRCUIT_BREAKER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisCircuitBreaker}.
 *
 * @author sean yu
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisCircuitBreakerTest {

	private static ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					RpcEnhancementAutoConfiguration.class,
					LoadBalancerAutoConfiguration.class,
					PolarisCircuitBreakerFeignClientAutoConfiguration.class,
					PolarisCircuitBreakerAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.enabled=true");

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;

	@BeforeClass
	public static void beforeClass() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.namespace"))
				.thenReturn(NAMESPACE_TEST);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.service"))
				.thenReturn(SERVICE_CIRCUIT_BREAKER);
	}

	@After
	public void after() {
		mockedApplicationContextAwareUtils.close();
	}

	@Test
	public void run() {
		this.contextRunner.run(context -> {
			PolarisCircuitBreakerFactory polarisCircuitBreakerFactory = context.getBean(PolarisCircuitBreakerFactory.class);
			CircuitBreaker cb = polarisCircuitBreakerFactory.create(SERVICE_CIRCUIT_BREAKER);

			assertThat(cb.run(() -> "foobar")).isEqualTo("foobar");

			assertThat((String) cb.run(() -> {
				throw new RuntimeException("boom");
			}, t -> "fallback")).isEqualTo("fallback");

		});
	}




}
