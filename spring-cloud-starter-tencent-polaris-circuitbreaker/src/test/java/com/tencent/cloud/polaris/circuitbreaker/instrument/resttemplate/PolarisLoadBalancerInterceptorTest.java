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

package com.tencent.cloud.polaris.circuitbreaker.instrument.resttemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for ${@link PolarisLoadBalancerInterceptor}.
 *
 * @author Shedfree Wu
 */
@ExtendWith(MockitoExtension.class)
class PolarisLoadBalancerInterceptorTest {

	@Mock
	private LoadBalancerClient loadBalancer;

	@Mock
	private LoadBalancerRequestFactory requestFactory;

	@Mock
	private EnhancedPluginRunner enhancedPluginRunner;

	@Mock
	private HttpRequest request;

	@Mock
	private ClientHttpRequestExecution execution;

	private PolarisLoadBalancerInterceptor interceptor;
	private byte[] body;

	@BeforeEach
	void setUp() {
		body = "test body".getBytes();
	}

	@Test
	void testInterceptWithEnhancedPlugin() throws IOException {
		// Arrange
		ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
		interceptor = new PolarisLoadBalancerInterceptor(loadBalancer, requestFactory, enhancedPluginRunner);
		URI uri = URI.create("http://test-service/path");
		when(request.getURI()).thenReturn(uri);
		when(loadBalancer.execute(any(), any())).thenReturn(mockResponse);

		// Act
		ClientHttpResponse response = interceptor.intercept(request, body, execution);

		// Assert
		Assertions.assertTrue(Objects.equals(mockResponse, response) || response instanceof PolarisCircuitBreakerHttpResponse);
	}

	@Test
	void testInterceptWithoutEnhancedPlugin() throws IOException {
		// Arrange
		ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
		interceptor = new PolarisLoadBalancerInterceptor(loadBalancer, requestFactory, null);
		URI uri = URI.create("http://test-service/path");
		when(request.getURI()).thenReturn(uri);
		when(loadBalancer.execute(any(), any())).thenReturn(mockResponse);

		// Act
		ClientHttpResponse response = interceptor.intercept(request, body, execution);

		// Assert
		Assertions.assertEquals(mockResponse, response);
	}

	@Test
	void testInterceptWithInvalidUri() {
		// Arrange
		interceptor = new PolarisLoadBalancerInterceptor(loadBalancer, requestFactory, enhancedPluginRunner);
		when(request.getURI()).thenReturn(URI.create("http:///path")); // Invalid URI without host

		// Act & Assert
		Exception exception = Assertions.assertThrows(IllegalStateException.class, () -> {
			interceptor.intercept(request, body, execution);
		});
		Assertions.assertTrue(exception.getMessage().contains("Request URI does not contain a valid hostname"));
	}

	@Test
	void testConstructor() {
		// Act
		interceptor = new PolarisLoadBalancerInterceptor(loadBalancer, requestFactory, enhancedPluginRunner);

		// Assert
		Assertions.assertNotNull(interceptor);
	}
}
