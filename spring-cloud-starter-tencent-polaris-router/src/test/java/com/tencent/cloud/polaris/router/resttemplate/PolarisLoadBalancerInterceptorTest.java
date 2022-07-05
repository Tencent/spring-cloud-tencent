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

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.router.RouterConstants;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.RouterLabelResolver;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * test for {@link PolarisLoadBalancerInterceptor}.
 *
 * @author lepdou, cheese8
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisLoadBalancerInterceptorTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	private static MockedStatic<MetadataContextHolder> mockedMetadataContextHolder;
	@Mock
	private LoadBalancerClient loadBalancerClient;
	@Mock
	private LoadBalancerRequestFactory loadBalancerRequestFactory;
	@Mock
	private RouterLabelResolver routerLabelResolver;
	@Mock
	private MetadataLocalProperties metadataLocalProperties;
	@Mock
	private RouterRuleLabelResolver routerRuleLabelResolver;

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
	public void testProxyRibbonLoadBalance() throws Exception {
		String callerService = "callerService";
		String calleeService = "calleeService";
		HttpRequest request = new MockedHttpRequest("http://" + calleeService + "/user/get");

		// mock local metadata
		Map<String, String> localMetadata = new HashMap<>();
		localMetadata.put("k1", "v1");
		localMetadata.put("k2", "v2");
		when(metadataLocalProperties.getContent()).thenReturn(localMetadata);

		// mock custom resolved from request
		Map<String, String> customResolvedLabels = new HashMap<>();
		customResolvedLabels.put("k3", "v3");
		customResolvedLabels.put("k4", "v4");
		when(routerLabelResolver.resolve(request, null)).thenReturn(customResolvedLabels);

		// mock expression rule labels

		Set<String> expressionKeys = new HashSet<>();
		expressionKeys.add("${http.method}");
		expressionKeys.add("${http.uri}");
		when(routerRuleLabelResolver.getExpressionLabelKeys(callerService, callerService, calleeService)).thenReturn(expressionKeys);

		MetadataContext metadataContext = Mockito.mock(MetadataContext.class);

		// mock transitive metadata
		Map<String, String> transitiveLabels = new HashMap<>();
		transitiveLabels.put("k1", "v1");
		transitiveLabels.put("k2", "v22");
		when(metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)).thenReturn(transitiveLabels);

		mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);

		LoadBalancerRequest<ClientHttpResponse> loadBalancerRequest = new MockedLoadBalancerRequest<>();
		when(loadBalancerRequestFactory.createRequest(request, null, null)).thenReturn(loadBalancerRequest);

		PolarisLoadBalancerInterceptor polarisLoadBalancerInterceptor = new PolarisLoadBalancerInterceptor(loadBalancerClient,
				loadBalancerRequestFactory, Collections.singletonList(routerLabelResolver), metadataLocalProperties, routerRuleLabelResolver);

		polarisLoadBalancerInterceptor.intercept(request, null, null);

		verify(metadataLocalProperties).getContent();
		verify(routerRuleLabelResolver).getExpressionLabelKeys(callerService, callerService, calleeService);
		verify(routerLabelResolver).resolve(request, null);
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
		when(metadataLocalProperties.getContent()).thenReturn(localMetadata);

		// mock custom resolved from request
		Map<String, String> customResolvedLabels = new HashMap<>();
		customResolvedLabels.put("k2", "v22");
		customResolvedLabels.put("k4", "v4");
		when(routerLabelResolver.resolve(request, null)).thenReturn(customResolvedLabels);

		// mock expression rule labels

		Set<String> expressionKeys = new HashSet<>();
		expressionKeys.add("${http.method}");
		expressionKeys.add("${http.uri}");
		when(routerRuleLabelResolver.getExpressionLabelKeys(callerService, callerService, calleeService)).thenReturn(expressionKeys);

		MetadataContext metadataContext = Mockito.mock(MetadataContext.class);

		// mock transitive metadata
		Map<String, String> transitiveLabels = new HashMap<>();
		transitiveLabels.put("k1", "v1");
		transitiveLabels.put("k2", "v22");
		when(metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)).thenReturn(transitiveLabels);

		mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);

		PolarisLoadBalancerInterceptor polarisLoadBalancerInterceptor = new PolarisLoadBalancerInterceptor(loadBalancerClient,
				loadBalancerRequestFactory, Collections.singletonList(routerLabelResolver), metadataLocalProperties, routerRuleLabelResolver);

		polarisLoadBalancerInterceptor.setLabelsToHeaders(request, null, calleeService);

		verify(metadataLocalProperties).getContent();
		verify(routerRuleLabelResolver).getExpressionLabelKeys(callerService, callerService, calleeService);
		verify(routerLabelResolver).resolve(request, null);

		Map<String, String> headers = JacksonUtils.deserialize2Map(URLDecoder.decode(request.getHeaders()
				.get(RouterConstants.ROUTER_LABEL_HEADER).get(0), StandardCharsets.UTF_8.name()));
		Assert.assertEquals("v1", headers.get("k1"));
		Assert.assertEquals("v22", headers.get("k2"));
		Assert.assertEquals("v4", headers.get("k4"));
		Assert.assertEquals("GET", headers.get("${http.method}"));
		Assert.assertEquals("/user/get", headers.get("${http.uri}"));
	}

	static class MockedLoadBalancerRequest<T> implements LoadBalancerRequest<T> {

		@Override
		public T apply(ServiceInstance instance) {
			return null;
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
