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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.APIConfig;
import com.tencent.polaris.api.config.global.GlobalConfig;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.ServiceCallResult;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link ReporterUtils}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
public class ReporterUtilsTest {

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
	public void testCreateServiceCallResult() {
		// mock target
		Target<?> target = mock(Target.class);
		doReturn(SERVICE_PROVIDER).when(target).name();

		// mock RequestTemplate.class
		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.feignTarget(target);
		try {
			requestTemplate.header(RouterConstant.ROUTER_LABEL_HEADER, URLEncoder.encode("{\"k1\":\"v1\",\"k2\":\"v2\"}", UTF_8));
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException("unsupported charset exception " + UTF_8);
		}

		// mock request
		Request request = mock(Request.class);
		doReturn(requestTemplate).when(request).requestTemplate();
		doReturn("http://1.1.1.1:2345/path").when(request).url();

		// mock request
		Response response = mock(Response.class);
		doReturn(502).when(response).status();

		ServiceCallResult serviceCallResult = ReporterUtils.createServiceCallResult(mockSDKContext(), request, response, 10L, RetStatus.RetSuccess);
		assertThat(serviceCallResult.getNamespace()).isEqualTo(NAMESPACE_TEST);
		assertThat(serviceCallResult.getService()).isEqualTo(SERVICE_PROVIDER);
		assertThat(serviceCallResult.getHost()).isEqualTo("1.1.1.1");
		assertThat(serviceCallResult.getPort()).isEqualTo(2345);
		assertThat(serviceCallResult.getRetStatus()).isEqualTo(RetStatus.RetSuccess);
		assertThat(serviceCallResult.getMethod()).isEqualTo("/path");
		assertThat(serviceCallResult.getCallerService().getNamespace()).isEqualTo(NAMESPACE_TEST);
		assertThat(serviceCallResult.getCallerService().getService()).isEqualTo(SERVICE_PROVIDER);
		assertThat(serviceCallResult.getLabels()).isEqualTo("k1:v1|k2:v2");
		assertThat(serviceCallResult.getRetCode()).isEqualTo(502);
		assertThat(serviceCallResult.getDelay()).isEqualTo(10L);
	}

	public static SDKContext mockSDKContext() {
		APIConfig apiConfig = mock(APIConfig.class);
		doReturn("127.0.0.1").when(apiConfig).getBindIP();
		GlobalConfig globalConfig = mock(GlobalConfig.class);
		doReturn(apiConfig).when(globalConfig).getAPI();
		Configuration configuration = mock(Configuration.class);
		doReturn(globalConfig).when(configuration).getGlobal();
		SDKContext context = mock(SDKContext.class);
		doReturn(configuration).when(context).getConfig();

		return context;
	}
}
