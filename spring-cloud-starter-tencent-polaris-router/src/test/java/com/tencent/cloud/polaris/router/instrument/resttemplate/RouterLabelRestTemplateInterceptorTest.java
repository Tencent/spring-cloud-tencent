/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.router.instrument.resttemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import feign.Request;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RouterLabelRestTemplateInterceptor}.
 *
 * @author liuye, Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RouterLabelRestTemplateInterceptorTest.TestApplication.class,
		properties = {"spring.cloud.polaris.namespace=test", "spring.application.name=test", "spring.cloud.gateway.enabled=false"})
public class RouterLabelRestTemplateInterceptorTest {

	@Mock
	private ClientHttpRequestExecution clientHttpRequestExecution;

	@Test
	public void testRouterLabel() throws Exception {
		RouterLabelRestTemplateInterceptor routerLabelRestTemplateInterceptor = new RouterLabelRestTemplateInterceptor();

		assertThat(routerLabelRestTemplateInterceptor.getOrder()).isEqualTo(OrderConstant.Client.RestTemplate.ROUTER_LABEL_INTERCEPTOR_ORDER);

		String calleeService = "calleeService";
		HttpRequest request = new MockedHttpRequest("http://" + calleeService + "/test/path?q1=a1");

		ClientHttpResponse mockedResponse = new MockClientHttpResponse(new byte[] {}, HttpStatus.OK);
		when(clientHttpRequestExecution.execute(eq(request), any())).thenReturn(mockedResponse);

		routerLabelRestTemplateInterceptor.intercept(request, null, clientHttpRequestExecution);

		// get message metadata container
		MetadataContainer metadataContainer = MetadataContextHolder.get()
				.getMetadataContainer(MetadataType.MESSAGE, false);
		// method
		assertThat(metadataContainer.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_METHOD)).isEqualTo(Request.HttpMethod.POST.toString());
		// path
		assertThat(metadataContainer.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_PATH)).isEqualTo("/test/path");
		// header
		assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, "uid")).isEqualTo("1000");
		// cookie
		assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, "k1")).isEqualTo("v1");
		// query
		assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, "q1")).isEqualTo("a1");
	}

	static class MockedHttpRequest implements HttpRequest {

		private final URI uri;

		private final HttpHeaders httpHeaders = new HttpHeaders();

		MockedHttpRequest(String url) {
			this.uri = URI.create(url);
			this.httpHeaders.add("uid", "1000");
			this.httpHeaders.add(HttpHeaderNames.COOKIE.toString(), "k1=v1");
		}

		@Override
		public HttpMethod getMethod() {
			return HttpMethod.POST;
		}

		@Override
		public URI getURI() {
			return uri;
		}

		@Override
		public Map<String, Object> getAttributes() {
			return new HashMap<>();
		}

		@Override
		public HttpHeaders getHeaders() {
			return httpHeaders;
		}
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
