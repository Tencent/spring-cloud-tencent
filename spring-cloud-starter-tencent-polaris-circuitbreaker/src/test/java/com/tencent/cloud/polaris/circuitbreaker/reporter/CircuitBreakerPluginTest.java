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

package com.tencent.cloud.polaris.circuitbreaker.reporter;

import java.net.URI;
import java.util.HashMap;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.circuitbreaker.PolarisCircuitBreaker;
import com.tencent.cloud.polaris.circuitbreaker.common.PolarisCircuitBreakerConfigBuilder;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.InvokeHandler;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CircuitBreakerPluginTest.
 *
 * @author Shedfree Wu
 */
@ExtendWith(MockitoExtension.class)
public class CircuitBreakerPluginTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	@InjectMocks
	private CircuitBreakerPlugin circuitBreakerPlugin;
	@Mock
	private CircuitBreakAPI circuitBreakAPI;
	@Mock
	private CircuitBreakerFactory circuitBreakerFactory;

	@Mock
	private ConsumerAPI consumerAPI;


	@BeforeAll
	static void beforeAll() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("unit-test");
	}

	@AfterAll
	static void afterAll() {
		mockedApplicationContextAwareUtils.close();
	}

	@BeforeEach
	void setUp() {
		MetadataContext.LOCAL_NAMESPACE = NAMESPACE_TEST;
		MetadataContext.LOCAL_SERVICE = SERVICE_PROVIDER;
	}

	@Test
	public void testGetName() {
		assertThat(circuitBreakerPlugin.getName()).isEqualTo(CircuitBreakerPlugin.class.getName());
	}

	@Test
	public void testType() {
		assertThat(circuitBreakerPlugin.getType()).isEqualTo(EnhancedPluginType.Client.PRE);
	}

	@Test
	public void testRun() throws Throwable {
		when(circuitBreakAPI.makeInvokeHandler(any())).thenReturn(new MockInvokeHandler());

		PolarisCircuitBreakerConfigBuilder polarisCircuitBreakerConfigBuilder = new PolarisCircuitBreakerConfigBuilder();
		PolarisCircuitBreaker polarisCircuitBreaker = new PolarisCircuitBreaker(polarisCircuitBreakerConfigBuilder.build(), consumerAPI, circuitBreakAPI);
		when(circuitBreakerFactory.create(anyString())).thenReturn(polarisCircuitBreaker);


		EnhancedPluginContext pluginContext = new EnhancedPluginContext();
		EnhancedRequestContext request = EnhancedRequestContext.builder()
				.httpMethod(HttpMethod.GET)
				.url(URI.create("http://0.0.0.0/"))
				.httpHeaders(new HttpHeaders())
				.build();
		EnhancedResponseContext response = EnhancedResponseContext.builder()
				.httpStatus(200)
				.build();
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
		serviceInstance.setServiceId(SERVICE_PROVIDER);

		pluginContext.setRequest(request);
		pluginContext.setResponse(response);
		pluginContext.setTargetServiceInstance(serviceInstance, null);
		pluginContext.setThrowable(new RuntimeException());

		assertThatThrownBy(() -> circuitBreakerPlugin.run(pluginContext)).isExactlyInstanceOf(CallAbortedException.class);
		circuitBreakerPlugin.getOrder();
		circuitBreakerPlugin.getName();
		circuitBreakerPlugin.getType();
	}

	@Test
	public void testRun2() throws Throwable {
		when(circuitBreakAPI.makeInvokeHandler(any())).thenReturn(new MockInvokeHandler2());

		PolarisCircuitBreakerConfigBuilder polarisCircuitBreakerConfigBuilder = new PolarisCircuitBreakerConfigBuilder();
		PolarisCircuitBreaker polarisCircuitBreaker = new PolarisCircuitBreaker(polarisCircuitBreakerConfigBuilder.build(), consumerAPI, circuitBreakAPI);
		when(circuitBreakerFactory.create(anyString())).thenReturn(polarisCircuitBreaker);


		EnhancedPluginContext pluginContext = new EnhancedPluginContext();
		EnhancedRequestContext request = EnhancedRequestContext.builder()
				.httpMethod(HttpMethod.GET)
				.url(URI.create("http://0.0.0.0/"))
				.httpHeaders(new HttpHeaders())
				.build();
		EnhancedResponseContext response = EnhancedResponseContext.builder()
				.httpStatus(200)
				.build();
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
		serviceInstance.setServiceId(SERVICE_PROVIDER);

		pluginContext.setRequest(request);
		pluginContext.setResponse(response);
		pluginContext.setTargetServiceInstance(serviceInstance, null);
		pluginContext.setThrowable(new RuntimeException());
		// no exception
		circuitBreakerPlugin.run(pluginContext);
	}

	@Test
	public void testHandlerThrowable() {
		// mock request
		EnhancedRequestContext request = mock(EnhancedRequestContext.class);
		// mock response
		EnhancedResponseContext response = mock(EnhancedResponseContext.class);

		EnhancedPluginContext context = new EnhancedPluginContext();
		context.setRequest(request);
		context.setResponse(response);
		circuitBreakerPlugin.handlerThrowable(context, new RuntimeException("Mock exception."));
	}

	static class MockInvokeHandler implements InvokeHandler {
		@Override
		public void acquirePermission() {
			throw new CallAbortedException("mock", new CircuitBreakerStatus.FallbackInfo(0, new HashMap<>(), ""));
		}

		@Override
		public void onSuccess(InvokeContext.ResponseContext responseContext) {

		}

		@Override
		public void onError(InvokeContext.ResponseContext responseContext) {

		}
	}

	static class MockInvokeHandler2 implements InvokeHandler {
		@Override
		public void acquirePermission() {
			return;
		}

		@Override
		public void onSuccess(InvokeContext.ResponseContext responseContext) {

		}

		@Override
		public void onError(InvokeContext.ResponseContext responseContext) {

		}
	}
}
