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

package com.tencent.cloud.rpc.enhancement.instrument.resttemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.metadata.core.MetadataType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.ServiceRequestWrapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link EnhancedRestTemplateWrapInterceptor}.
 *
 * @author Shedfree Wu
 */
@ExtendWith(MockitoExtension.class)
class EnhancedRestTemplateWrapInterceptorTest {

	@Mock
	private EnhancedPluginRunner pluginRunner;

	@Mock
	private LoadBalancerClient delegate;

	@Mock
	private HttpRequest request;

	@Mock
	private ClientHttpResponse response;

	@Mock
	private ServiceRequestWrapper serviceRequestWrapper;

	@Mock
	private ServiceInstance localServiceInstance;

	private EnhancedRestTemplateWrapInterceptor interceptor;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		interceptor = new EnhancedRestTemplateWrapInterceptor(pluginRunner, delegate);
	}

	@Test
	void testInterceptWithNormalRequest() throws IOException {
		// Arrange
		URI uri = URI.create("http://test-service/api");
		HttpHeaders headers = new HttpHeaders();
		headers.add("test-header", "test-value");

		when(request.getURI()).thenReturn(uri);
		when(request.getHeaders()).thenReturn(headers);
		when(request.getMethod()).thenReturn(HttpMethod.GET);
		when(pluginRunner.getLocalServiceInstance()).thenReturn(localServiceInstance);
		when(delegate.execute(any(), any())).thenReturn(response);
		when(response.getRawStatusCode()).thenReturn(200);
		when(response.getHeaders()).thenReturn(new HttpHeaders());

		// Act
		interceptor.intercept(request, "test-service", mock(LoadBalancerRequest.class));

		// Assert
		verify(pluginRunner).run(eq(EnhancedPluginType.Client.PRE), any(EnhancedPluginContext.class));

		// Verify context setup
		ArgumentCaptor<EnhancedPluginContext> contextCaptor = ArgumentCaptor.forClass(EnhancedPluginContext.class);
		verify(pluginRunner).run(eq(EnhancedPluginType.Client.PRE), contextCaptor.capture());

		EnhancedPluginContext capturedContext = contextCaptor.getValue();
		Assertions.assertEquals(uri, capturedContext.getRequest().getUrl());
		Assertions.assertEquals(uri, capturedContext.getRequest().getServiceUrl());
		Assertions.assertEquals(headers, capturedContext.getRequest().getHttpHeaders());
		Assertions.assertEquals(HttpMethod.GET, capturedContext.getRequest().getHttpMethod());
		Assertions.assertEquals(localServiceInstance, capturedContext.getLocalServiceInstance());
	}

	@Test
	void testInterceptWithServiceRequestWrapper() throws IOException {
		// Arrange
		URI originalUri = URI.create("http://original-service/api");
		URI wrappedUri = URI.create("http://wrapped-service/api");
		HttpHeaders headers = new HttpHeaders();

		when(serviceRequestWrapper.getURI()).thenReturn(wrappedUri);
		when(serviceRequestWrapper.getRequest()).thenReturn(request);
		when(serviceRequestWrapper.getHeaders()).thenReturn(headers);
		when(serviceRequestWrapper.getMethod()).thenReturn(HttpMethod.POST);
		when(request.getURI()).thenReturn(originalUri);
		when(pluginRunner.getLocalServiceInstance()).thenReturn(localServiceInstance);
		when(delegate.execute(any(), any())).thenReturn(response);
		when(response.getRawStatusCode()).thenReturn(200);
		when(response.getHeaders()).thenReturn(new HttpHeaders());

		// Act
		interceptor.intercept(serviceRequestWrapper, "test-service", mock(LoadBalancerRequest.class));

		// Assert
		verify(pluginRunner).run(eq(EnhancedPluginType.Client.PRE), any(EnhancedPluginContext.class));

		ArgumentCaptor<EnhancedPluginContext> contextCaptor = ArgumentCaptor.forClass(EnhancedPluginContext.class);
		verify(pluginRunner).run(eq(EnhancedPluginType.Client.PRE), contextCaptor.capture());

		EnhancedPluginContext capturedContext = contextCaptor.getValue();
		Assertions.assertEquals(wrappedUri, capturedContext.getRequest().getUrl());
		Assertions.assertEquals(originalUri, capturedContext.getRequest().getServiceUrl());
		Assertions.assertEquals(serviceRequestWrapper, capturedContext.getOriginRequest());
	}

	@Test
	void testInterceptWithFallback() throws IOException {
		// Arrange
		URI originalUri = URI.create("http://original-service/api");
		URI wrappedUri = URI.create("http://wrapped-service/api");
		HttpHeaders headers = new HttpHeaders();

		CallAbortedException abortedException = new CallAbortedException("test-error", null);

		when(serviceRequestWrapper.getURI()).thenReturn(wrappedUri);
		when(serviceRequestWrapper.getRequest()).thenReturn(request);
		when(serviceRequestWrapper.getHeaders()).thenReturn(headers);
		when(serviceRequestWrapper.getMethod()).thenReturn(HttpMethod.POST);
		when(request.getURI()).thenReturn(originalUri);
		when(pluginRunner.getLocalServiceInstance()).thenReturn(localServiceInstance);
		doThrow(abortedException)
				.when(pluginRunner)
				.run(any(), any());

		Object fallbackResponse = new MockClientHttpResponse();
		MetadataContextHolder.get().getMetadataContainer(MetadataType.APPLICATION, true).
				putMetadataObjectValue(ContextConstant.CircuitBreaker.CIRCUIT_BREAKER_FALLBACK_HTTP_RESPONSE, fallbackResponse);

		// Act
		interceptor.intercept(serviceRequestWrapper, "test-service", mock(LoadBalancerRequest.class));

		// Assert
		verify(pluginRunner).run(eq(EnhancedPluginType.Client.PRE), any(EnhancedPluginContext.class));

		ArgumentCaptor<EnhancedPluginContext> contextCaptor = ArgumentCaptor.forClass(EnhancedPluginContext.class);
		verify(pluginRunner).run(eq(EnhancedPluginType.Client.PRE), contextCaptor.capture());

		EnhancedPluginContext capturedContext = contextCaptor.getValue();
		Assertions.assertEquals(wrappedUri, capturedContext.getRequest().getUrl());
		Assertions.assertEquals(originalUri, capturedContext.getRequest().getServiceUrl());
		Assertions.assertEquals(serviceRequestWrapper, capturedContext.getOriginRequest());
	}

	@Test
	void testInterceptWithNoFallback() {
		// Arrange
		URI originalUri = URI.create("http://original-service/api");
		URI wrappedUri = URI.create("http://wrapped-service/api");
		HttpHeaders headers = new HttpHeaders();

		CallAbortedException abortedException = new CallAbortedException("test-error", null);

		when(serviceRequestWrapper.getURI()).thenReturn(wrappedUri);
		when(serviceRequestWrapper.getRequest()).thenReturn(request);
		when(serviceRequestWrapper.getHeaders()).thenReturn(headers);
		when(serviceRequestWrapper.getMethod()).thenReturn(HttpMethod.POST);
		when(request.getURI()).thenReturn(originalUri);
		when(pluginRunner.getLocalServiceInstance()).thenReturn(localServiceInstance);
		doThrow(abortedException)
				.when(pluginRunner)
				.run(any(), any());
		// Act
		Assertions.assertThrows(CallAbortedException.class, () -> {
			interceptor.intercept(serviceRequestWrapper, "test-service", mock(LoadBalancerRequest.class));
		});
	}

	@Test
	void testInterceptWithNullLocalServiceInstance() throws IOException {
		// Arrange
		URI uri = URI.create("http://test-service/api");
		when(request.getURI()).thenReturn(uri);
		when(request.getHeaders()).thenReturn(new HttpHeaders());
		when(request.getMethod()).thenReturn(HttpMethod.GET);
		when(pluginRunner.getLocalServiceInstance()).thenReturn(null);
		when(delegate.execute(any(), any())).thenReturn(response);
		when(response.getRawStatusCode()).thenReturn(200);
		when(response.getHeaders()).thenReturn(new HttpHeaders());

		// Act
		interceptor.intercept(request, "test-service", mock(LoadBalancerRequest.class));

		// Assert
		verify(pluginRunner).run(eq(EnhancedPluginType.Client.PRE), any(EnhancedPluginContext.class));

		ArgumentCaptor<EnhancedPluginContext> contextCaptor = ArgumentCaptor.forClass(EnhancedPluginContext.class);
		verify(pluginRunner).run(eq(EnhancedPluginType.Client.PRE), contextCaptor.capture());

		EnhancedPluginContext capturedContext = contextCaptor.getValue();
		assertThat(capturedContext.getLocalServiceInstance()).isNull();
	}

	@Test
	void testExceptionHandling() throws IOException {
		// Arrange
		LoadBalancerRequest<ClientHttpResponse> loadBalancerRequest = mock(LoadBalancerRequest.class);
		IOException expectedException = new IOException("Test exception");
		when(delegate.execute(anyString(), any(LoadBalancerRequest.class)))
				.thenThrow(expectedException);

		// Act & Assert
		Exception actualException = Assertions.assertThrows(IOException.class, () -> {
			interceptor.intercept(request, "test-service", loadBalancerRequest);
		});

		// Verify exception handling
		verify(pluginRunner, times(1))
				.run(eq(EnhancedPluginType.Client.EXCEPTION), any(EnhancedPluginContext.class));

		// Verify finally block is executed
		verify(pluginRunner, times(1))
				.run(eq(EnhancedPluginType.Client.FINALLY), any(EnhancedPluginContext.class));

		// Verify the thrown exception is the same
		Assertions.assertEquals(expectedException, actualException);
	}

	static class MockClientHttpResponse implements ClientHttpResponse {
		@Override
		public HttpStatus getStatusCode() throws IOException {
			return null;
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return 0;
		}

		@Override
		public String getStatusText() throws IOException {
			return null;
		}

		@Override
		public void close() {

		}

		@Override
		public InputStream getBody() throws IOException {
			return null;
		}

		@Override
		public HttpHeaders getHeaders() {
			return null;
		}
	}
}
