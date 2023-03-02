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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RouterContextFactory}
 *
 * @author lepdou 2022-10-09
 */
@ExtendWith(MockitoExtension.class)
public class RouterContextFactoryTest {

	@Mock
	private SpringWebRouterLabelResolver springWebRouterLabelResolver;
	@Mock
	private StaticMetadataManager staticMetadataManager;
	@Mock
	private RouterRuleLabelResolver routerRuleLabelResolver;
	@Mock
	private PolarisContextProperties polarisContextProperties;

	@Test
	public void testRouterContext() {
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
		when(springWebRouterLabelResolver.resolve(request, null, expressionKeys)).thenReturn(customResolvedLabels);

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(callerService);

			MetadataContext metadataContext = Mockito.mock(MetadataContext.class);

			// mock transitive metadata
			Map<String, String> transitiveLabels = new HashMap<>();
			transitiveLabels.put("k1", "v1");
			transitiveLabels.put("k2", "v22");
			when(metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)).thenReturn(transitiveLabels);

			try (MockedStatic<MetadataContextHolder> mockedMetadataContextHolder = Mockito.mockStatic(MetadataContextHolder.class)) {
				mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);

				RouterContextFactory routerContextFactory = new RouterContextFactory(Collections.singletonList(springWebRouterLabelResolver),
						staticMetadataManager, routerRuleLabelResolver, polarisContextProperties);

				PolarisRouterContext routerContext = routerContextFactory.create(request, null, calleeService);

				verify(staticMetadataManager).getMergedStaticMetadata();
				verify(routerRuleLabelResolver).getExpressionLabelKeys(callerService, callerService, calleeService);
				verify(springWebRouterLabelResolver).resolve(request, null, expressionKeys);

				assertThat(routerContext.getLabels(RouterConstant.TRANSITIVE_LABELS).get("k1")).isEqualTo("v1");
				assertThat(routerContext.getLabels(RouterConstant.TRANSITIVE_LABELS).get("k2")).isEqualTo("v22");
				assertThat(routerContext.getLabels(RouterConstant.ROUTER_LABELS).get("k1")).isEqualTo("v1");
				assertThat(routerContext.getLabels(RouterConstant.ROUTER_LABELS).get("k2")).isEqualTo("v22");
				assertThat(routerContext.getLabels(RouterConstant.ROUTER_LABELS).get("k4")).isEqualTo("v4");
				assertThat(routerContext.getLabels(RouterConstant.ROUTER_LABELS)
						.get("${http.method}")).isEqualTo("GET");
				assertThat(routerContext.getLabels(RouterConstant.ROUTER_LABELS)
						.get("${http.uri}")).isEqualTo("/user/get");
			}
		}
	}

	static class MockedHttpRequest implements HttpRequest {

		private final URI uri;

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
			return new HttpHeaders();
		}
	}
}
