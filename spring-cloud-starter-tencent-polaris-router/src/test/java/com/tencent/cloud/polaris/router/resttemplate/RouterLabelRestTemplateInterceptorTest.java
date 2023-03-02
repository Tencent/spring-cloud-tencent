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
 *
 */

package com.tencent.cloud.polaris.router.resttemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.common.util.expresstion.SpringWebExpressionLabelUtils;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RouterLabelRestTemplateInterceptor}.
 *
 * @author liuye, Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
public class RouterLabelRestTemplateInterceptorTest {

	private static final String testNamespaceAndService = "testNamespaceAndService";
	@Mock

	private SpringWebRouterLabelResolver routerLabelResolver;
	@Mock
	private StaticMetadataManager staticMetadataManager;
	@Mock
	private RouterRuleLabelResolver routerRuleLabelResolver;
	@Mock
	private PolarisContextProperties polarisContextProperties;

	@Mock
	private ClientHttpRequestExecution clientHttpRequestExecution;


	@Test
	public void testRouterContext() throws Exception {
		try (
				MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = mockStatic(ApplicationContextAwareUtils.class);
				MockedStatic<MetadataContextHolder> mockedMetadataContextHolder = mockStatic(MetadataContextHolder.class)
		) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testNamespaceAndService);

			String calleeService = "calleeService";
			HttpRequest request = new MockedHttpRequest("http://" + calleeService + "/user/get");

			// mock local metadata
			Map<String, String> localMetadata = new HashMap<>();
			localMetadata.put("k1", "v1");
			localMetadata.put("k2", "v2");
			when(staticMetadataManager.getMergedStaticMetadata()).thenReturn(localMetadata);
			Map<String, String> routerLabels = new HashMap<>(localMetadata);

			// mock expression rule labels
			Set<String> expressionKeys = new HashSet<>();
			expressionKeys.add("${http.method}");
			expressionKeys.add("${http.uri}");
			when(routerRuleLabelResolver.getExpressionLabelKeys(testNamespaceAndService, testNamespaceAndService, calleeService)).thenReturn(expressionKeys);
			routerLabels.putAll(SpringWebExpressionLabelUtils.resolve(request, expressionKeys));

			// mock custom resolved from request
			Map<String, String> customResolvedLabels = new HashMap<>();
			customResolvedLabels.put("k2", "v22");
			customResolvedLabels.put("k4", "v4");
			when(routerLabelResolver.resolve(request, null, expressionKeys)).thenReturn(customResolvedLabels);
			routerLabels.putAll(customResolvedLabels);

			MetadataContext metadataContext = Mockito.mock(MetadataContext.class);

			// mock transitive metadata
			Map<String, String> transitiveLabels = new HashMap<>();
			transitiveLabels.put("k1", "v1");
			transitiveLabels.put("k2", "v22");
			when(metadataContext.getTransitiveMetadata()).thenReturn(transitiveLabels);
			routerLabels.putAll(transitiveLabels);

			mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);

			RouterLabelRestTemplateInterceptor routerLabelRestTemplateInterceptor = new RouterLabelRestTemplateInterceptor(
					Collections.singletonList(routerLabelResolver), staticMetadataManager, routerRuleLabelResolver, polarisContextProperties);

			ClientHttpResponse mockedResponse = new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
			when(clientHttpRequestExecution.execute(eq(request), any())).thenReturn(mockedResponse);

			assertThat(routerLabelRestTemplateInterceptor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

			routerLabelRestTemplateInterceptor.intercept(request, null, clientHttpRequestExecution);

			verify(staticMetadataManager).getMergedStaticMetadata();
			verify(routerRuleLabelResolver).getExpressionLabelKeys(testNamespaceAndService, testNamespaceAndService, calleeService);
			verify(routerLabelResolver).resolve(request, null, expressionKeys);


			Map<String, String> headers = JacksonUtils.deserialize2Map(URLDecoder.decode(Objects.requireNonNull(request.getHeaders()
					.get(RouterConstant.ROUTER_LABEL_HEADER)).get(0), "UTF-8"));
			assertThat("v1").isEqualTo(headers.get("k1"));
			assertThat("v22").isEqualTo(headers.get("k2"));
			assertThat("v4").isEqualTo(headers.get("k4"));
			assertThat("GET").isEqualTo(headers.get("${http.method}"));
			assertThat("/user/get").isEqualTo(headers.get("${http.uri}"));
			String encodedLabelsContent;
			try {
				encodedLabelsContent = URLEncoder.encode(JacksonUtils.serialize2Json(routerLabels), UTF_8);
			}
			catch (UnsupportedEncodingException e) {
				throw new RuntimeException("unsupported charset exception " + UTF_8);
			}
			assertThat(mockedResponse.getHeaders().get(RouterConstant.ROUTER_LABEL_HEADER).get(0))
					.isEqualTo(encodedLabelsContent);
		}
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
