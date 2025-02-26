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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.openfeign.FallbackFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Tests for {@link PolarisFeignCircuitBreaker}.
 */
public class PolarisFeignCircuitBreakerTest {

	private PolarisFeignCircuitBreaker.Builder builder;

	@BeforeEach
	public void setUp() {
		builder = PolarisFeignCircuitBreaker.builder();
	}

	@Test
	public void testBuilderNotNull() {
		Assertions.assertNotNull(builder);
	}

	@Test
	public void testTargetWithFallback() {
		// Mock the target
		Class<MyService> targetType = MyService.class;
		String name = "myService";
		Target<MyService> target = mock(Target.class);

		// mock return value
		when(target.type()).thenReturn(targetType);
		when(target.name()).thenReturn(name);

		// Mock the fallback
		MyService fallback = mock(MyService.class);
		when(fallback.sayHello()).thenReturn("Fallback Hello");

		// Call the target method
		MyService result = builder.target(target, fallback);

		// Verify that the result is not null and the fallback factory is used
		Assertions.assertNotNull(result);
		Assertions.assertEquals("Fallback Hello", result.sayHello());
	}

	@Test
	public void testTargetWithFallbackFactory() {
		// Mock the target and fallback factory
		Class<MyService> targetType = MyService.class;
		String name = "myService";
		Target<MyService> target = mock(Target.class);

		// mock return value
		when(target.type()).thenReturn(targetType);
		when(target.name()).thenReturn(name);

		FallbackFactory<MyService> fallbackFactory = mock(FallbackFactory.class);

		// Mock the fallback from the factory
		MyService fallback = mock(MyService.class);
		when(fallback.sayHello()).thenReturn("Fallback Hello");
		when(fallbackFactory.create(any())).thenReturn(fallback);

		// Call the target method
		MyService result = builder.target(target, fallbackFactory);

		// Verify that the result is not null and the fallback factory is used
		Assertions.assertNotNull(result);
		Assertions.assertEquals("Fallback Hello", result.sayHello());
	}

	@Test
	public void testTargetWithoutFallback() {
		// Mock the target
		Class<MyService> targetType = MyService.class;
		String name = "myService";
		Target<MyService> target = mock(Target.class);

		// mock return value
		when(target.type()).thenReturn(targetType);
		when(target.name()).thenReturn(name);

		// Call the target method
		MyService result = builder.target(target);

		// Verify that the result is not null
		Assertions.assertNotNull(result);
		// Additional verifications can be added here based on the implementation
	}

	@Test
	public void testBuildWithNullableFallbackFactory() {
		// Call the build method with a null fallback factory
		Feign feign = builder.build(null);

		// Verify that the Feign instance is not null
		Assertions.assertNotNull(feign);
		// Additional verifications can be added here based on the implementation
	}

	public interface MyService {
		@RequestLine("GET /hello")
		String sayHello();
	}
}
