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

import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignContext;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RetStatus;
import feign.Request;
import feign.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link ExceptionPolarisReporter}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ExceptionPolarisReporterTest {

	private static MockedStatic<ReporterUtils> mockedReporterUtils;
	@Mock
	private ConsumerAPI consumerAPI;
	@Mock
	private RpcEnhancementReporterProperties reporterProperties;
	@InjectMocks
	private ExceptionPolarisReporter exceptionPolarisReporter;

	@BeforeClass
	public static void beforeClass() {
		mockedReporterUtils = Mockito.mockStatic(ReporterUtils.class);
		mockedReporterUtils.when(() -> ReporterUtils.createServiceCallResult(any(Request.class), any(RetStatus.class)))
				.thenReturn(null);
	}

	@AfterClass
	public static void afterClass() {
		mockedReporterUtils.close();
	}

	@Test
	public void testGetName() {
		assertThat(exceptionPolarisReporter.getName()).isEqualTo(ExceptionPolarisReporter.class.getName());
	}

	@Test
	public void testType() {
		assertThat(exceptionPolarisReporter.getType()).isEqualTo(EnhancedFeignPluginType.EXCEPTION);
	}

	@Test
	public void testRun() {
		// mock request
		Request request = mock(Request.class);
		// mock response
		Response response = mock(Response.class);

		EnhancedFeignContext context = new EnhancedFeignContext();
		context.setRequest(request);
		context.setResponse(response);
		exceptionPolarisReporter.run(context);
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
		exceptionPolarisReporter.handlerThrowable(context, new RuntimeException("Mock exception."));
	}
}
