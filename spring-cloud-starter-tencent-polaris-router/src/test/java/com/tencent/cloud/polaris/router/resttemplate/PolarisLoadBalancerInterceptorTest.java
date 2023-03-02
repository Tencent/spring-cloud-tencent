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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * test for {@link PolarisLoadBalancerInterceptor}.
 *
 * @author lepdou 2022-05-26
 */
@ExtendWith(MockitoExtension.class)
public class PolarisLoadBalancerInterceptorTest {

	@Mock
	private RibbonLoadBalancerClient loadBalancerClient;
	@Mock
	private LoadBalancerRequestFactory loadBalancerRequestFactory;
	@Mock
	private RouterContextFactory routerContextFactory;

	@Test
	public void testProxyRibbonLoadBalance() throws Exception {
		String callerService = "callerService";
		String calleeService = "calleeService";
		HttpRequest request = new MockedHttpRequest("http://" + calleeService + "/user/get");

		PolarisRouterContext routerContext = new PolarisRouterContext();
		Map<String, String> routerLabels = new HashMap<>();
		routerLabels.put("k1", "v1");
		routerLabels.put("k2", "v12");
		routerContext.putLabels(RouterConstant.ROUTER_LABELS, routerLabels);

		when(routerContextFactory.create(request, null, calleeService)).thenReturn(routerContext);

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(callerService);

			MetadataContext metadataContext = Mockito.mock(MetadataContext.class);

			try (MockedStatic<MetadataContextHolder> mockedMetadataContextHolder = Mockito.mockStatic(MetadataContextHolder.class)) {
				mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);


				LoadBalancerRequest<ClientHttpResponse> loadBalancerRequest = new MockedLoadBalancerRequest<>();
				when(loadBalancerRequestFactory.createRequest(request, null, null)).thenReturn(loadBalancerRequest);

				PolarisLoadBalancerInterceptor polarisLoadBalancerInterceptor = new PolarisLoadBalancerInterceptor(loadBalancerClient,
						loadBalancerRequestFactory, routerContextFactory);

				ClientHttpResponse mockedResponse = new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
				when(loadBalancerClient.execute(eq(calleeService), eq(loadBalancerRequest), any(PolarisRouterContext.class))).thenReturn(mockedResponse);

				polarisLoadBalancerInterceptor.intercept(request, null, null);

				String encodedLabelsContent;
				try {
					encodedLabelsContent = URLEncoder.encode(JacksonUtils.serialize2Json(routerLabels), UTF_8);
				}
				catch (UnsupportedEncodingException e) {
					throw new RuntimeException("unsupported charset exception " + UTF_8);
				}
				assertThat(mockedResponse.getHeaders().get(RouterConstant.ROUTER_LABELS).get(0))
						.isEqualTo(encodedLabelsContent);
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
				notRibbonLoadBalancerClient, loadBalancerRequestFactory, routerContextFactory);

		ClientHttpResponse response = polarisLoadBalancerInterceptor.intercept(request, null, null);

		assertThat(response).isEqualTo(mockedResponse);
		verify(loadBalancerRequestFactory).createRequest(request, null, null);
		verify(notRibbonLoadBalancerClient).execute(calleeService, loadBalancerRequest);

	}

	static class MockedLoadBalancerRequest<T> implements LoadBalancerRequest<T> {

		@Override
		public T apply(ServiceInstance instance) {
			return null;
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
