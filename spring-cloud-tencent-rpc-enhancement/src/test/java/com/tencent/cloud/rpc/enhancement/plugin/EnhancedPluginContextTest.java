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

package com.tencent.cloud.rpc.enhancement.plugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.plugin.reporter.ExceptionPolarisReporter;
import com.tencent.cloud.rpc.enhancement.plugin.reporter.SuccessPolarisReporter;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.APIConfig;
import com.tencent.polaris.api.config.global.GlobalConfig;
import com.tencent.polaris.api.core.ConsumerAPI;
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

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link EnhancedPluginContext}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
public class EnhancedPluginContextTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	@Mock
	private RpcEnhancementReporterProperties reporterProperties;
	@Mock
	private SDKContext sdkContext;
	@Mock
	private ConsumerAPI consumerAPI;
	@Mock
	private Registration registration;

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
	public void testGetAndSet() throws Throwable {
		EnhancedRequestContext requestContext = new EnhancedRequestContext();
		requestContext.setHttpHeaders(new HttpHeaders());
		requestContext.setUrl(new URI("/"));
		requestContext.setHttpMethod(HttpMethod.GET);

		EnhancedRequestContext requestContext1 = EnhancedRequestContext.builder()
				.httpHeaders(requestContext.getHttpHeaders())
				.url(requestContext.getUrl())
				.httpMethod(requestContext.getHttpMethod())
				.build();
		assertThat(requestContext1.getUrl()).isEqualTo(requestContext.getUrl());

		EnhancedResponseContext responseContext = new EnhancedResponseContext();
		responseContext.setHttpStatus(200);
		responseContext.setHttpHeaders(new HttpHeaders());

		EnhancedResponseContext responseContext1 = EnhancedResponseContext.builder()
				.httpStatus(responseContext.getHttpStatus())
				.httpHeaders(responseContext.getHttpHeaders())
				.build();
		assertThat(responseContext1.getHttpStatus()).isEqualTo(responseContext.getHttpStatus());

		EnhancedPluginContext enhancedPluginContext = new EnhancedPluginContext();
		enhancedPluginContext.setRequest(requestContext);
		enhancedPluginContext.setResponse(responseContext);
		enhancedPluginContext.setTargetServiceInstance(new DefaultServiceInstance(), null);
		enhancedPluginContext.setThrowable(mock(Exception.class));
		enhancedPluginContext.setDelay(0);
		assertThat(enhancedPluginContext.getRequest()).isNotNull();
		assertThat(enhancedPluginContext.getResponse()).isNotNull();
		assertThat(enhancedPluginContext.getTargetServiceInstance()).isNotNull();
		assertThat(enhancedPluginContext.getThrowable()).isNotNull();
		assertThat(enhancedPluginContext.getDelay()).isNotNull();

		EnhancedPlugin enhancedPlugin = new SuccessPolarisReporter(reporterProperties, consumerAPI);
		EnhancedPlugin enhancedPlugin1 = new ExceptionPolarisReporter(reporterProperties, consumerAPI);
		EnhancedPluginRunner enhancedPluginRunner = new DefaultEnhancedPluginRunner(Arrays.asList(enhancedPlugin, enhancedPlugin1), registration, sdkContext);
		enhancedPluginRunner.run(EnhancedPluginType.Client.POST, enhancedPluginContext);

		assertThat(enhancedPluginRunner.getLocalServiceInstance()).isEqualTo(registration);

		EnhancedPlugin enhancedPlugin2 = mock(EnhancedPlugin.class);
		doThrow(new RuntimeException()).when(enhancedPlugin2).run(any());
		doReturn(EnhancedPluginType.Client.POST).when(enhancedPlugin2).getType();

		APIConfig apiConfig = mock(APIConfig.class);
		doReturn("0.0.0.0").when(apiConfig).getBindIP();

		GlobalConfig globalConfig = mock(GlobalConfig.class);
		doReturn(apiConfig).when(globalConfig).getAPI();

		Configuration configuration = mock(Configuration.class);
		doReturn(globalConfig).when(configuration).getGlobal();

		doReturn(configuration).when(sdkContext).getConfig();

		enhancedPluginRunner = new DefaultEnhancedPluginRunner(Collections.singletonList(enhancedPlugin2), null, sdkContext);
		enhancedPluginRunner.run(EnhancedPluginType.Client.POST, enhancedPluginContext);
	}

	@Test
	public void testSetTargetServiceInstance() throws URISyntaxException {
		EnhancedPluginContext enhancedPluginContext = new EnhancedPluginContext();

		// targetServiceInstance != null
		DefaultServiceInstance testDefaultServiceInstance = new DefaultServiceInstance();
		testDefaultServiceInstance.setPort(1);
		enhancedPluginContext.setTargetServiceInstance(testDefaultServiceInstance, null);
		assertThat(enhancedPluginContext.getTargetServiceInstance().getPort()).isEqualTo(1);

		// targetServiceInstance == null && url != null
		enhancedPluginContext.setTargetServiceInstance(null, new URI("https://www.qq.com"));
		assertThat(enhancedPluginContext.getTargetServiceInstance().getPort()).isEqualTo(443);

		// targetServiceInstance == null && url == null
		enhancedPluginContext.setTargetServiceInstance(null, null);
		assertThat(enhancedPluginContext.getTargetServiceInstance().getPort()).isEqualTo(0);
	}
}
