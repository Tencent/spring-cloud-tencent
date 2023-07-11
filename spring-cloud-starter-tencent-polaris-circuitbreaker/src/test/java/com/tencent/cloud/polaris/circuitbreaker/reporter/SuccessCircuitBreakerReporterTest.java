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

package com.tencent.cloud.polaris.circuitbreaker.reporter;

import java.net.URI;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.client.api.SDKContext;
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
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * SuccessCircuitBreakerReporterTest.
 *
 * @author sean yu
 */
@ExtendWith(MockitoExtension.class)
public class SuccessCircuitBreakerReporterTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	@Mock
	private SDKContext sdkContext;
	@Mock
	private RpcEnhancementReporterProperties reporterProperties;
	@InjectMocks
	private SuccessCircuitBreakerReporter successCircuitBreakerReporter;
	@Mock
	private CircuitBreakAPI circuitBreakAPI;

	@BeforeAll
	static void beforeAll() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("unit-test");
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		RpcEnhancementReporterProperties reporterProperties = mock(RpcEnhancementReporterProperties.class);
		doReturn(reporterProperties)
				.when(applicationContext).getBean(RpcEnhancementReporterProperties.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext)
				.thenReturn(applicationContext);
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
		assertThat(successCircuitBreakerReporter.getName()).isEqualTo(SuccessCircuitBreakerReporter.class.getName());
	}

	@Test
	public void testType() {
		assertThat(successCircuitBreakerReporter.getType()).isEqualTo(EnhancedPluginType.Client.POST);
	}

	@Test
	public void testRun() throws Throwable {

		EnhancedPluginContext context = mock(EnhancedPluginContext.class);
		// test not report
		successCircuitBreakerReporter.run(context);
		verify(context, times(0)).getRequest();

		doReturn(true).when(reporterProperties).isEnabled();

		EnhancedPluginContext pluginContext = new EnhancedPluginContext();
		EnhancedRequestContext request = EnhancedRequestContext.builder()
				.httpMethod(HttpMethod.GET)
				.url(URI.create("http://0.0.0.0/"))
				.build();
		EnhancedResponseContext response = EnhancedResponseContext.builder()
				.httpStatus(200)
				.build();
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
		serviceInstance.setServiceId(SERVICE_PROVIDER);

		pluginContext.setRequest(request);
		pluginContext.setResponse(response);
		pluginContext.setTargetServiceInstance(serviceInstance, null);

		successCircuitBreakerReporter.run(pluginContext);
		successCircuitBreakerReporter.getOrder();
		successCircuitBreakerReporter.getName();
		successCircuitBreakerReporter.getType();
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
		successCircuitBreakerReporter.handlerThrowable(context, new RuntimeException("Mock exception."));
	}
}
