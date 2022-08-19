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

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * test for {@link PolarisLoadBalancerInterceptor}.
 *
 * @author lepdou 2022-05-26
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisLoadBalancerInterceptorTest {

	@Mock
	private RibbonLoadBalancerClient loadBalancerClient;
	@Mock
	private LoadBalancerRequestFactory loadBalancerRequestFactory;
	@Mock
	private SpringWebRouterLabelResolver routerLabelResolver;
	@Mock
	private StaticMetadataManager staticMetadataManager;
	@Mock
	private RouterRuleLabelResolver routerRuleLabelResolver;

	@Test
	public void testProxyRibbonLoadBalance() throws Exception {
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
		customResolvedLabels.put("k3", "v3");
		customResolvedLabels.put("k4", "v4");
		when(routerLabelResolver.resolve(request, null, expressionKeys)).thenReturn(customResolvedLabels);


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

				LoadBalancerRequest<ClientHttpResponse> loadBalancerRequest = new MockedLoadBalancerRequest<>();
				when(loadBalancerRequestFactory.createRequest(request, null, null)).thenReturn(loadBalancerRequest);

				PolarisLoadBalancerInterceptor polarisLoadBalancerInterceptor = new PolarisLoadBalancerInterceptor(loadBalancerClient,
						loadBalancerRequestFactory, Collections.singletonList(routerLabelResolver), staticMetadataManager, routerRuleLabelResolver);

				polarisLoadBalancerInterceptor.intercept(request, null, null);

				verify(staticMetadataManager).getMergedStaticMetadata();
				verify(routerRuleLabelResolver).getExpressionLabelKeys(callerService, callerService, calleeService);
				verify(routerLabelResolver).resolve(request, null, expressionKeys);
			}
		}
	}

	@Test
	public void testNotProxyRibbonLoadBalance() throws IOException {
		String calleeService = "calleeService";
		HttpRequest request = new MockedHttpRequest("http://" + calleeService + "/user/get");

		LoadBalancerRequest<ClientHttpResponse> loadBalancerRequest = new MockedLoadBalancerRequest<>();
		when(loadBalancerRequestFactory.createRequest(request, null, null)).thenReturn(loadBalancerRequest);

		LoadBalancerClient notRibbonLoadBalancerClient = Mockito.mock(LoadBalancerClient.class);
		ClientHttpResponse mockedResponse = new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
		when(notRibbonLoadBalancerClient.execute(calleeService, loadBalancerRequest)).thenReturn(mockedResponse);

		PolarisLoadBalancerInterceptor polarisLoadBalancerInterceptor = new PolarisLoadBalancerInterceptor(
				notRibbonLoadBalancerClient, loadBalancerRequestFactory,
				Collections.singletonList(routerLabelResolver), staticMetadataManager,
				routerRuleLabelResolver);

		ClientHttpResponse response = polarisLoadBalancerInterceptor.intercept(request, null, null);

		Assert.assertEquals(mockedResponse, response);
		verify(loadBalancerRequestFactory).createRequest(request, null, null);
		verify(notRibbonLoadBalancerClient).execute(calleeService, loadBalancerRequest);

	}

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
		when(routerLabelResolver.resolve(request, null, expressionKeys)).thenReturn(customResolvedLabels);

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

				PolarisLoadBalancerInterceptor polarisLoadBalancerInterceptor =
						new PolarisLoadBalancerInterceptor(loadBalancerClient, loadBalancerRequestFactory,
								Collections.singletonList(routerLabelResolver), staticMetadataManager,
								routerRuleLabelResolver);

				PolarisRouterContext routerContext = polarisLoadBalancerInterceptor.genRouterContext(request, null, calleeService);

				verify(staticMetadataManager).getMergedStaticMetadata();
				verify(routerRuleLabelResolver).getExpressionLabelKeys(callerService, callerService, calleeService);
				verify(routerLabelResolver).resolve(request, null, expressionKeys);

				Assert.assertEquals("v1", routerContext.getLabels(PolarisRouterContext.TRANSITIVE_LABELS).get("k1"));
				Assert.assertEquals("v22", routerContext.getLabels(PolarisRouterContext.TRANSITIVE_LABELS).get("k2"));
				Assert.assertEquals("v1", routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS).get("k1"));
				Assert.assertEquals("v22", routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS).get("k2"));
				Assert.assertEquals("v4", routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS).get("k4"));
				Assert.assertEquals("GET", routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS)
						.get("${http.method}"));
				Assert.assertEquals("/user/get", routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS)
						.get("${http.uri}"));
			}
		}
	}

	static class MockedLoadBalancerRequest<T> implements LoadBalancerRequest<T> {

		@Override
		public T apply(ServiceInstance instance) {
			return null;
		}
	}

	static class MockedHttpRequest implements HttpRequest {

		private URI uri;

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
			return null;
		}
	}
}
