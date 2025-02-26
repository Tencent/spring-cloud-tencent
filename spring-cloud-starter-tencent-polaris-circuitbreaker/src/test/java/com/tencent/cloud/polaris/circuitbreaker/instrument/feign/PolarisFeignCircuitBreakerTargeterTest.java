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

package com.tencent.cloud.polaris.circuitbreaker.instrument.feign;

import feign.Feign;
import feign.RequestLine;
import feign.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;

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

	@Mock
	FeignClientFactory feignClientFactory;

	@Mock
	private FeignClientFactoryBean factory;

	@Test
	public void testTarget() {
		PolarisFeignCircuitBreakerTargeter targeter = new PolarisFeignCircuitBreakerTargeter();
		targeter.target(new FeignClientFactoryBean(), new Feign.Builder(), new FeignClientFactory(), new Target.HardCodedTarget<>(TestApi.class, "/test"));
	}

	@Test
	public void testTarget2() {
		PolarisFeignCircuitBreakerTargeter targeter = new PolarisFeignCircuitBreakerTargeter();
		FeignClientFactoryBean feignClientFactoryBean = mock(FeignClientFactoryBean.class);
		doReturn(TestApi.class).when(feignClientFactoryBean).getFallback();
		doReturn("test").when(feignClientFactoryBean).getName();
		doReturn(null).when(feignClientFactory).getInstance("test", TestApi.class);
		assertThatThrownBy(() -> {
			targeter.target(feignClientFactoryBean, new PolarisFeignCircuitBreaker.Builder(), feignClientFactory, new Target.HardCodedTarget<>(TestApi.class, "/test"));
		}).isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void testTarget3() {
		PolarisFeignCircuitBreakerTargeter targeter = new PolarisFeignCircuitBreakerTargeter();
		FeignClientFactoryBean feignClientFactoryBean = mock(FeignClientFactoryBean.class);
		doReturn(void.class).when(feignClientFactoryBean).getFallback();
		doReturn(TestApi.class).when(feignClientFactoryBean).getFallbackFactory();
		doReturn("test").when(feignClientFactoryBean).getName();
		doReturn(Object.class).when(feignClientFactory).getInstance("test", TestApi.class);
		assertThatThrownBy(() -> {
			targeter.target(feignClientFactoryBean, new PolarisFeignCircuitBreaker.Builder(), feignClientFactory, new Target.HardCodedTarget<>(TestApi.class, "/test"));
		}).isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void testTarget4() {
		PolarisFeignCircuitBreakerTargeter targeter = new PolarisFeignCircuitBreakerTargeter();
		FeignClientFactoryBean feignClientFactoryBean = mock(FeignClientFactoryBean.class);
		// no fallback and no fallback factory
		doReturn(void.class).when(feignClientFactoryBean).getFallback();
		doReturn(void.class).when(feignClientFactoryBean).getFallbackFactory();
		doReturn("test").when(feignClientFactoryBean).getName();

		targeter.target(feignClientFactoryBean, new PolarisFeignCircuitBreaker.Builder(), feignClientFactory, new Target.HardCodedTarget<>(TestApi.class, "/test"));
	}

	@Test
	public void testTargetWithFallbackFactory() {
		PolarisFeignCircuitBreakerTargeter targeter = new PolarisFeignCircuitBreakerTargeter();
		FeignClientFactoryBean feignClientFactoryBean = mock(FeignClientFactoryBean.class);

		doReturn(void.class).when(feignClientFactoryBean).getFallback();
		doReturn(PolarisCircuitBreakerFallbackFactory.class).when(feignClientFactoryBean).getFallbackFactory();
		doReturn("test").when(feignClientFactoryBean).getName();

		doReturn(new PolarisCircuitBreakerFallbackFactory()).when(feignClientFactory)
				.getInstance("test", PolarisCircuitBreakerFallbackFactory.class);

		targeter.target(feignClientFactoryBean, new PolarisFeignCircuitBreaker.Builder(), feignClientFactory, new Target.HardCodedTarget<>(TestApi.class, "/test"));
	}

	@Test
	public void testTargetWithFallback() {
		PolarisFeignCircuitBreakerTargeter targeter = new PolarisFeignCircuitBreakerTargeter();
		FeignClientFactoryBean feignClientFactoryBean = mock(FeignClientFactoryBean.class);

		doReturn(TestApiFallback.class).when(feignClientFactoryBean).getFallback();
		doReturn("test").when(feignClientFactoryBean).getName();


		doReturn(new PolarisCircuitBreakerFallbackFactory()).when(feignClientFactory)
				.getInstance("test", TestApiFallback.class);

		targeter.target(feignClientFactoryBean, new PolarisFeignCircuitBreaker.Builder(), feignClientFactory, new Target.HardCodedTarget<>(TestApi.class, "/test"));
	}

	interface TestApi {
		@RequestLine("GET /test")
		void test();
	}

	static class TestApiFallback implements TestApi {
		@Override
		public void test() {
			// fallback implementation
		}
	}

	public static class PolarisCircuitBreakerFallbackFactory implements FallbackFactory<TestApi> {
		@Override
		public TestApi create(Throwable cause) {
			return new TestApiFallback();
		}
	}

}
