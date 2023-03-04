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

package com.tencent.cloud.polaris.circuitbreaker;

import java.util.Arrays;
import java.util.Collections;

import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.circuitbreaker.common.PolarisCircuitBreakerConfigBuilder;
import com.tencent.cloud.polaris.circuitbreaker.config.ReactivePolarisCircuitBreakerAutoConfiguration;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementAutoConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_CIRCUIT_BREAKER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ReactivePolarisCircuitBreaker}.
 *
 * @author sean yu
 */
@ExtendWith(MockitoExtension.class)
public class ReactivePolarisCircuitBreakerTest {

	private final ApplicationContextRunner reactiveContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					RpcEnhancementAutoConfiguration.class,
					LoadBalancerAutoConfiguration.class,
					ReactivePolarisCircuitBreakerAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.circuitbreaker.enabled=true");

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;

	@BeforeAll
	public static void beforeClass() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.namespace"))
				.thenReturn(NAMESPACE_TEST);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.service"))
				.thenReturn(SERVICE_CIRCUIT_BREAKER);
	}

	@AfterAll
	public static void afterAll() {
		mockedApplicationContextAwareUtils.close();
	}

	@Test
	public void run() {
		this.reactiveContextRunner.run(context -> {
			ReactivePolarisCircuitBreakerFactory polarisCircuitBreakerFactory = context.getBean(ReactivePolarisCircuitBreakerFactory.class);
			ReactiveCircuitBreaker cb = polarisCircuitBreakerFactory.create(SERVICE_CIRCUIT_BREAKER);

			PolarisCircuitBreakerConfigBuilder.PolarisCircuitBreakerConfiguration configuration =
					polarisCircuitBreakerFactory.configBuilder(SERVICE_CIRCUIT_BREAKER).build();

			polarisCircuitBreakerFactory.configureDefault(id -> configuration);

			assertThat(Mono.just("foobar").transform(cb::run).block()).isEqualTo("foobar");

			assertThat(Mono.error(new RuntimeException("boom")).transform(it -> cb.run(it, t -> Mono.just("fallback")))
					.block()).isEqualTo("fallback");

			assertThat(Flux.just("foobar", "hello world").transform(cb::run).collectList().block())
					.isEqualTo(Arrays.asList("foobar", "hello world"));

			assertThat(Flux.error(new RuntimeException("boom")).transform(it -> cb.run(it, t -> Flux.just("fallback")))
					.collectList().block()).isEqualTo(Collections.singletonList("fallback"));
		});
	}

}
