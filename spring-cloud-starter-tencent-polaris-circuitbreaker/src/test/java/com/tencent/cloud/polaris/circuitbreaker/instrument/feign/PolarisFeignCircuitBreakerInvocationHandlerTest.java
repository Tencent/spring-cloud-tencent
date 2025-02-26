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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.polaris.circuitbreaker.exception.FallbackWrapperException;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import feign.InvocationHandlerFactory;
import feign.Target;
import feign.codec.Decoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.openfeign.FallbackFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for ${@link PolarisFeignCircuitBreakerInvocationHandler}.
 *
 * @author Shedfree Wu
 */
@ExtendWith(MockitoExtension.class)
class PolarisFeignCircuitBreakerInvocationHandlerTest {

	@Mock
	private Target<?> target;

	@Mock
	private InvocationHandlerFactory.MethodHandler methodHandler;

	@Mock
	private FallbackFactory<TestInterface> fallbackFactory;

	@Mock
	private Decoder decoder;

	private Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;
	private PolarisFeignCircuitBreakerInvocationHandler handler;

	private Method testMethod;

	@BeforeEach
	void setUp() throws Exception {
		dispatch = new HashMap<>();

		testMethod = TestInterface.class.getDeclaredMethod("testMethod");
		dispatch.put(testMethod, methodHandler);

		handler = new PolarisFeignCircuitBreakerInvocationHandler(
				target,
				dispatch,
				fallbackFactory,
				decoder
		);
	}

	@Test
	void testConstructorWithNullTarget() {
		Assertions.assertThrows(NullPointerException.class, () ->
				new PolarisFeignCircuitBreakerInvocationHandler(
						null, dispatch, fallbackFactory, decoder
				)
		);
	}

	@Test
	void testConstructorWithNullDispatch() {
		Assertions.assertThrows(NullPointerException.class, () ->
				new PolarisFeignCircuitBreakerInvocationHandler(
						target, null, fallbackFactory, decoder
				)
		);
	}

	@Test
	void testToFallbackMethod() throws Exception {
		Method method = TestInterface.class.getMethod("testMethod");
		Map<Method, InvocationHandlerFactory.MethodHandler> testDispatch = new HashMap<>();
		testDispatch.put(method, methodHandler);

		Map<Method, Method> result = PolarisFeignCircuitBreakerInvocationHandler.toFallbackMethod(testDispatch);

		Assertions.assertNotNull(result);
		Assertions.assertTrue(result.containsKey(method));
		Assertions.assertEquals(method, result.get(method));
	}

	@Test
	void testEqualsMethod() throws Throwable {
		Method equalsMethod = Object.class.getMethod("equals", Object.class);
		Object mockProxy = mock(Object.class);

		// Test equals with null
		Assertions.assertFalse((Boolean) handler.invoke(mockProxy, equalsMethod, new Object[] {null}));

		// Test equals with non-proxy object
		Assertions.assertFalse((Boolean) handler.invoke(mockProxy, equalsMethod, new Object[] {new Object()}));
	}

	@Test
	void testToStringMethod() throws Throwable {
		Method toStringMethod = Object.class.getMethod("toString");
		Object mockProxy = mock(Object.class);
		when(target.toString()).thenReturn("TestTarget");

		Assertions.assertEquals("TestTarget", handler.invoke(mockProxy, toStringMethod, null));
	}

	@Test
	void testCustomFallbackFactoryWithFallbackError() throws Throwable {
		// Arrange
		handler = new PolarisFeignCircuitBreakerInvocationHandler(target, dispatch, fallbackFactory, decoder);
		Exception originalException = new RuntimeException("Original error");

		when(methodHandler.invoke(any())).thenThrow(originalException);
		TestImpl testImpl = new TestImpl();
		when(fallbackFactory.create(any())).thenReturn(testImpl);

		// Act
		assertThatThrownBy(() -> handler.invoke(null, testMethod, new Object[] {})).isInstanceOf(FallbackWrapperException.class);
	}

	@Test
	void testCustomFallbackFactoryWithFallbackError2() throws Throwable {
		// Arrange
		handler = new PolarisFeignCircuitBreakerInvocationHandler(target, dispatch, fallbackFactory, decoder);
		Exception originalException = new RuntimeException("Original error");

		when(methodHandler.invoke(any())).thenThrow(originalException);
		TestImpl2 testImpl = new TestImpl2();
		when(fallbackFactory.create(any())).thenReturn(testImpl);

		// Act
		assertThatThrownBy(() -> handler.invoke(null, testMethod, new Object[] {})).
				isInstanceOf(RuntimeException.class).hasMessage("test");
	}

	@Test
	void testDefaultFallbackCreation() throws Throwable {
		// Arrange
		handler = new PolarisFeignCircuitBreakerInvocationHandler(target, dispatch, null, decoder);
		CircuitBreakerStatus.FallbackInfo fallbackInfo = new CircuitBreakerStatus.FallbackInfo(200, new HashMap<>(), "mock body");
		CallAbortedException originalException = new CallAbortedException("test rule", fallbackInfo);

		Object expected = new Object();
		when(methodHandler.invoke(any())).thenThrow(originalException);
		when(decoder.decode(any(), any())).thenReturn(expected);

		// Act
		Object result = handler.invoke(null, testMethod, new Object[] {});

		// Verify
		Assertions.assertEquals(expected, result);
	}

	@Test
	void testEquals() {
		PolarisFeignCircuitBreakerInvocationHandler testHandler = new PolarisFeignCircuitBreakerInvocationHandler(
				target,
				dispatch,
				fallbackFactory,
				decoder
		);

		Assertions.assertEquals(handler, testHandler);
		Assertions.assertEquals(handler.hashCode(), testHandler.hashCode());
	}

	interface TestInterface {
		String testMethod() throws InvocationTargetException;
	}

	static class TestImpl implements TestInterface {

		@Override
		public String testMethod() throws InvocationTargetException {
			throw new InvocationTargetException(new RuntimeException("test"));
		}
	}

	static class TestImpl2 implements TestInterface {

		@Override
		public String testMethod() throws InvocationTargetException {
			throw new RuntimeException("test");
		}
	}
}
