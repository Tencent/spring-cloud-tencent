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

import java.lang.reflect.Method;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.ReflectionUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import feign.Target;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author sean yu
 */
@ExtendWith(MockitoExtension.class)
public class PolarisCircuitBreakerNameResolverTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;

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
	public void test() {
		Target target = mock(Target.class);
		doReturn("test-svc").when(target).name();
		Method method = ReflectionUtils.findMethod(PolarisCircuitBreakerNameResolverTest.class, "mockRequestMapping");
		PolarisCircuitBreakerNameResolver resolver = new PolarisCircuitBreakerNameResolver();
		String polarisCircuitBreakerName = resolver.resolveCircuitBreakerName("test", target, method);
		assertThat(polarisCircuitBreakerName).isEqualTo("Test#test-svc");
	}

	@Test
	public void test2() {
		Target target = mock(Target.class);
		doReturn("test-svc").when(target).name();
		Method method = ReflectionUtils.findMethod(PolarisCircuitBreakerNameResolverTest.class, "mockRequestMapping2");
		PolarisCircuitBreakerNameResolver resolver = new PolarisCircuitBreakerNameResolver();
		String polarisCircuitBreakerName = resolver.resolveCircuitBreakerName("test", target, method);
		assertThat(polarisCircuitBreakerName).isEqualTo("Test#test-svc#/");
	}

	@Test
	public void test3() {
		Target target = mock(Target.class);
		doReturn("test-svc").when(target).name();
		Method method = ReflectionUtils.findMethod(PolarisCircuitBreakerNameResolverTest.class, "mockRequestMapping3");
		PolarisCircuitBreakerNameResolver resolver = new PolarisCircuitBreakerNameResolver();
		String polarisCircuitBreakerName = resolver.resolveCircuitBreakerName("test", target, method);
		assertThat(polarisCircuitBreakerName).isEqualTo("Test#test-svc#/");
	}


	@RequestMapping
	public void mockRequestMapping() {

	}

	@RequestMapping(path = "/")
	public void mockRequestMapping2() {

	}

	@RequestMapping("/")
	public void mockRequestMapping3() {

	}

}
