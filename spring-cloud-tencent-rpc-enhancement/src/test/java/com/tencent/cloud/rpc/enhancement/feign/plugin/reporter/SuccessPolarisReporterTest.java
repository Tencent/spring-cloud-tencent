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

package com.tencent.cloud.rpc.enhancement.feign.plugin.reporter;

import java.util.HashMap;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignContext;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.APIConfig;
import com.tencent.polaris.api.config.global.GlobalConfig;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.client.api.SDKContext;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.Target;
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

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link SuccessPolarisReporter}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
public class SuccessPolarisReporterTest {
	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	@Mock
	private SDKContext sdkContext;
	@Mock
	private ConsumerAPI consumerAPI;
	@Mock
	private CircuitBreakAPI circuitBreakAPI;
	@Mock
	private RpcEnhancementReporterProperties reporterProperties;
	@InjectMocks
	private SuccessPolarisReporter successPolarisReporter;

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
		assertThat(successPolarisReporter.getName()).isEqualTo(SuccessPolarisReporter.class.getName());
	}

	@Test
	public void testType() {
		assertThat(successPolarisReporter.getType()).isEqualTo(EnhancedFeignPluginType.POST);
	}

	@Test
	public void testRun() {

		EnhancedFeignContext context = mock(EnhancedFeignContext.class);
		// test not report
		successPolarisReporter.run(context);
		verify(context, times(0)).getRequest();

		doReturn(true).when(reporterProperties).isEnabled();
		// mock target
		Target<?> target = mock(Target.class);
		doReturn(SERVICE_PROVIDER).when(target).name();

		APIConfig apiConfig = mock(APIConfig.class);
		doReturn("0.0.0.0").when(apiConfig).getBindIP();

		GlobalConfig globalConfig = mock(GlobalConfig.class);
		doReturn(apiConfig).when(globalConfig).getAPI();

		Configuration configuration = mock(Configuration.class);
		doReturn(globalConfig).when(configuration).getGlobal();

		doReturn(configuration).when(sdkContext).getConfig();

		// mock RequestTemplate.class
		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.feignTarget(target);

		EnhancedFeignContext feignContext = new EnhancedFeignContext();
		Request request = Request.create(Request.HttpMethod.GET, "http://0.0.0.0/", new HashMap<>(), null, null, requestTemplate);
		Response response = Response.builder()
				.request(request)
				.build();
		feignContext.setRequest(request);
		feignContext.setResponse(response);
		successPolarisReporter.run(feignContext);
		successPolarisReporter.getOrder();
	}

	@Test
	public void testHandlerThrowable() {
		// mock request
		Request request = mock(Request.class);
		// mock response
		Response response = mock(Response.class);

		EnhancedFeignContext context = new EnhancedFeignContext();
		context.setRequest(request);
		context.setResponse(response);
		successPolarisReporter.handlerThrowable(context, new RuntimeException("Mock exception."));
	}
}
