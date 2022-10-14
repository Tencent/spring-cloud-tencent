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

package com.tencent.cloud.polaris.router.resttemplate;


import java.net.URI;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.constant.RouterConstants;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * test for {@link RouterLabelRestTemplateInterceptor}
 * @author liuye 2022-09-16
 */
@RunWith(MockitoJUnitRunner.class)
public class RouterLabelRestTemplateInterceptorTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	private static MockedStatic<MetadataContextHolder> mockedMetadataContextHolder;
	@Mock
	private SpringWebRouterLabelResolver routerLabelResolver;
	@Mock
	private StaticMetadataManager staticMetadataManager;
	@Mock
	private RouterRuleLabelResolver routerRuleLabelResolver;
	@Mock
	private PolarisContextProperties polarisContextProperties;

	@BeforeClass
	public static void beforeClass() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("callerService");

		mockedMetadataContextHolder = Mockito.mockStatic(MetadataContextHolder.class);
	}

	@AfterClass
	public static void afterClass() {
		mockedApplicationContextAwareUtils.close();
		mockedMetadataContextHolder.close();
	}

	@Test
	public void testRouterContext() throws Exception {
		String callerService = "callerService";
		String calleeService = "calleeService";
		HttpRequest request = new MockedHttpRequest("http://" + calleeService + "/user/get");

		// mock local metadata
		Map<String, String> localMetadata = new HashMap<>();
		localMetadata.put("k1", "v1");
		localMetadata.put("k2", "v2");
		when(staticMetadataManager.getMergedStaticMetadata()).thenReturn(localMetadata);

		// mock expression rule labels

		Set<String> expressionKeys = new HashSet<>();
		expressionKeys.add("${http.method}");
		expressionKeys.add("${http.uri}");
		when(routerRuleLabelResolver.getExpressionLabelKeys(callerService, callerService, calleeService)).thenReturn(expressionKeys);

		// mock custom resolved from request
		Map<String, String> customResolvedLabels = new HashMap<>();
		customResolvedLabels.put("k2", "v22");
		customResolvedLabels.put("k4", "v4");
		when(routerLabelResolver.resolve(request, null, expressionKeys)).thenReturn(customResolvedLabels);

		MetadataContext metadataContext = Mockito.mock(MetadataContext.class);

		// mock transitive metadata
		Map<String, String> transitiveLabels = new HashMap<>();
		transitiveLabels.put("k1", "v1");
		transitiveLabels.put("k2", "v22");
		when(metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)).thenReturn(transitiveLabels);

		mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);

		RouterLabelRestTemplateInterceptor routerLabelRestTemplateInterceptor = new RouterLabelRestTemplateInterceptor(
				Collections.singletonList(routerLabelResolver), staticMetadataManager, routerRuleLabelResolver, polarisContextProperties);

		routerLabelRestTemplateInterceptor.setLabelsToHeaders(request, null, calleeService);

		verify(staticMetadataManager).getMergedStaticMetadata();
		verify(routerRuleLabelResolver).getExpressionLabelKeys(callerService, callerService, calleeService);
		verify(routerLabelResolver).resolve(request, null, expressionKeys);


		Map<String, String> headers = JacksonUtils.deserialize2Map(URLDecoder.decode(request.getHeaders()
				.get(RouterConstants.ROUTER_LABEL_HEADER).get(0), "UTF-8"));
		Assertions.assertThat("v1").isEqualTo(headers.get("k1"));
		Assertions.assertThat("v22").isEqualTo(headers.get("k2"));
		Assertions.assertThat("v4").isEqualTo(headers.get("k4"));
		Assertions.assertThat("GET").isEqualTo(headers.get("${http.method}"));
		Assertions.assertThat("/user/get").isEqualTo(headers.get("${http.uri}"));
	}

	static class MockedHttpRequest implements HttpRequest {

		private URI uri;

		private HttpHeaders httpHeaders = new HttpHeaders();

		MockedHttpRequest(String url) {
			this.uri = URI.create(url);
		}

		@Override
		public String getMethodValue() {
			return HttpMethod.GET.name();
		}

		@Override
		public URI getURI() {
			return uri;
		}

		@Override
		public HttpHeaders getHeaders() {
			return httpHeaders;
		}
	}
}
