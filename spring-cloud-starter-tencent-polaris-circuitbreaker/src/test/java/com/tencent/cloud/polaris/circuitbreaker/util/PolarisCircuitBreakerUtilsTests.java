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

package com.tencent.cloud.polaris.circuitbreaker.util;

import java.util.HashMap;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.circuitbreaker.common.PolarisCircuitBreakerConfigBuilder;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

public class PolarisCircuitBreakerUtilsTests {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;


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
	public void testReportStatus() {
		ConsumerAPI consumerAPI = Mockito.mock(ConsumerAPI.class);
		Mockito.doNothing().when(consumerAPI).updateServiceCallResult(new ServiceCallResult());
		PolarisCircuitBreakerConfigBuilder.PolarisCircuitBreakerConfiguration conf = new PolarisCircuitBreakerConfigBuilder.PolarisCircuitBreakerConfiguration();
		PolarisCircuitBreakerUtils.reportStatus(consumerAPI, conf, new CallAbortedException("mock", new CircuitBreakerStatus.FallbackInfo(0, new HashMap<>(), "")));
	}

	@Test
	public void testResolveCircuitBreakerId() {
		assertThat(PolarisCircuitBreakerUtils.resolveCircuitBreakerId("test_svc")).isEqualTo(new String[]{NAMESPACE_TEST, "test_svc", ""});
		assertThat(PolarisCircuitBreakerUtils.resolveCircuitBreakerId("test_svc#test_path")).isEqualTo(new String[]{NAMESPACE_TEST, "test_svc", "test_path"});
		assertThat(PolarisCircuitBreakerUtils.resolveCircuitBreakerId("test_ns#test_svc#test_path")).isEqualTo(new String[]{"test_ns", "test_svc", "test_path"});
	}

}
