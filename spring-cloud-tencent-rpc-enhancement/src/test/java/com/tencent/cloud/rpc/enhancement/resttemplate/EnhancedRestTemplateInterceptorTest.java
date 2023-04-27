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

package com.tencent.cloud.rpc.enhancement.resttemplate;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.plugin.DefaultEnhancedPluginRunner;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class EnhancedRestTemplateInterceptorTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	@Mock
	private RpcEnhancementReporterProperties reporterProperties;
	@Mock
	private SDKContext sdkContext;
	@Mock
	Registration registration;
	@Mock
	private ClientHttpRequestExecution mockClientHttpRequestExecution;
	@Mock
	private ClientHttpResponse mockClientHttpResponse;
	@Mock
	private HttpRequest mockHttpRequest;
	@Mock
	private HttpHeaders mockHttpHeaders;

	@BeforeAll
	static void beforeAll() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("unit-test");
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		MetadataLocalProperties metadataLocalProperties = mock(MetadataLocalProperties.class);
		StaticMetadataManager staticMetadataManager = mock(StaticMetadataManager.class);
		doReturn(metadataLocalProperties).when(applicationContext).getBean(MetadataLocalProperties.class);
		doReturn(staticMetadataManager).when(applicationContext).getBean(StaticMetadataManager.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext).thenReturn(applicationContext);
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
	public void testRun() throws IOException, URISyntaxException {

		ClientHttpResponse actualResult;
		final byte[] inputBody = null;

		URI uri = new URI("http://0.0.0.0/");
		doReturn(uri).when(mockHttpRequest).getURI();
		doReturn(HttpMethod.GET).when(mockHttpRequest).getMethod();
		doReturn(mockHttpHeaders).when(mockHttpRequest).getHeaders();
		doReturn(mockClientHttpResponse).when(mockClientHttpRequestExecution).execute(mockHttpRequest, inputBody);

		EnhancedRestTemplateInterceptor reporter = new EnhancedRestTemplateInterceptor(new DefaultEnhancedPluginRunner(new ArrayList<>(), registration, null));
		actualResult = reporter.intercept(mockHttpRequest, inputBody, mockClientHttpRequestExecution);
		assertThat(actualResult).isEqualTo(mockClientHttpResponse);

		actualResult = reporter.intercept(mockHttpRequest, inputBody, mockClientHttpRequestExecution);
		assertThat(actualResult).isEqualTo(mockClientHttpResponse);

		doThrow(new SocketTimeoutException()).when(mockClientHttpRequestExecution).execute(mockHttpRequest, inputBody);
		assertThatThrownBy(() -> reporter.intercept(mockHttpRequest, inputBody, mockClientHttpRequestExecution)).isInstanceOf(SocketTimeoutException.class);
	}

}
