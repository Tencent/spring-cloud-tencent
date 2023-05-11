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

import feign.Feign;
import feign.RequestLine;
import feign.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.cloud.openfeign.FeignContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * PolarisFeignCircuitBreakerTargeterTest.
 *
 * @author sean yu
 */
@ExtendWith(MockitoExtension.class)
public class PolarisFeignCircuitBreakerTargeterTest {

	@Mock
	CircuitBreakerFactory circuitBreakerFactory;

	@Mock
	CircuitBreakerNameResolver circuitBreakerNameResolver;

	@Test
	public void testTarget() {
		PolarisFeignCircuitBreakerTargeter targeter = new PolarisFeignCircuitBreakerTargeter(circuitBreakerFactory, circuitBreakerNameResolver);
		targeter.target(new FeignClientFactoryBean(), new Feign.Builder(), new FeignContext(), new Target.HardCodedTarget<>(TestApi.class, "/test"));
	}

	@Test
	public void testTarget2() {
		PolarisFeignCircuitBreakerTargeter targeter = new PolarisFeignCircuitBreakerTargeter(circuitBreakerFactory, circuitBreakerNameResolver);
		FeignClientFactoryBean feignClientFactoryBean = mock(FeignClientFactoryBean.class);
		doReturn(TestApi.class).when(feignClientFactoryBean).getFallback();
		doReturn("test").when(feignClientFactoryBean).getName();
		FeignContext feignClientFactory = mock(FeignContext.class);
		doReturn(null).when(feignClientFactory).getInstance("test", TestApi.class);
		assertThatThrownBy(() -> {
			targeter.target(feignClientFactoryBean, new PolarisFeignCircuitBreaker.Builder(), feignClientFactory, new Target.HardCodedTarget<>(TestApi.class, "/test"));
		}).isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void testTarget3() {
		PolarisFeignCircuitBreakerTargeter targeter = new PolarisFeignCircuitBreakerTargeter(circuitBreakerFactory, circuitBreakerNameResolver);
		FeignClientFactoryBean feignClientFactoryBean = mock(FeignClientFactoryBean.class);
		doReturn(void.class).when(feignClientFactoryBean).getFallback();
		doReturn(TestApi.class).when(feignClientFactoryBean).getFallbackFactory();
		doReturn("test").when(feignClientFactoryBean).getName();
		FeignContext feignClientFactory = mock(FeignContext.class);
		doReturn(Object.class).when(feignClientFactory).getInstance("test", TestApi.class);
		assertThatThrownBy(() -> {
			targeter.target(feignClientFactoryBean, new PolarisFeignCircuitBreaker.Builder(), feignClientFactory, new Target.HardCodedTarget<>(TestApi.class, "/test"));
		}).isInstanceOf(IllegalStateException.class);
	}

	interface TestApi {
		@RequestLine("GET /test")
		void test();
	}

}
