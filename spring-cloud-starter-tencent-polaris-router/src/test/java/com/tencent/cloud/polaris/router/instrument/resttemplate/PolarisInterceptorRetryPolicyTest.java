/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tencent.cloud.polaris.router.instrument.resttemplate;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisInterceptorRetryPolicy}.
 *
 * @author fishtailfu 2026-10-19
 */
public class PolarisInterceptorRetryPolicyTest {

	private static final String SERVICE_NAME = "order-service";

	private HttpRequest request;

	private LoadBalancedRetryPolicy retryPolicy;

	private PolarisRouterContext routerContext;

	private ServiceInstance serviceInstance;

	@BeforeEach
	public void setUp() {
		request = mock(HttpRequest.class);
		when(request.getHeaders()).thenReturn(new HttpHeaders());

		retryPolicy = mock(LoadBalancedRetryPolicy.class);
		serviceInstance = mock(ServiceInstance.class);

		Map<String, String> labels = new HashMap<>();
		labels.put("k1", "v1");
		routerContext = new PolarisRouterContext();
		routerContext.putLabels(RouterConstant.ROUTER_LABELS, labels);
	}

	@Test
	public void testFirstAttemptWithRibbonChooser() {
		RibbonLoadBalancerClient ribbonClient = mock(RibbonLoadBalancerClient.class);
		when(ribbonClient.choose(eq(SERVICE_NAME), eq(request))).thenReturn(serviceInstance);

		LoadBalancedRetryContext retryContext = mock(LoadBalancedRetryContext.class);
		when(retryContext.getRetryCount()).thenReturn(0);
		when(retryContext.getServiceInstance()).thenReturn(null);

		PolarisInterceptorRetryPolicy policy = new PolarisInterceptorRetryPolicy(request, retryPolicy,
				ribbonClient, SERVICE_NAME, routerContext);

		assertThat(policy.canRetry(retryContext)).isTrue();

		// router context should be set to request header
		assertThat(request.getHeaders().containsKey(RouterConstant.HEADER_ROUTER_CONTEXT)).isTrue();

		// chosen instance should be set to retry context
		verify(retryContext).setServiceInstance(serviceInstance);
	}

	@Test
	public void testRetryWithRibbonChooser() {
		RibbonLoadBalancerClient ribbonClient = mock(RibbonLoadBalancerClient.class);

		LoadBalancedRetryContext retryContext = mock(LoadBalancedRetryContext.class);
		when(retryContext.getRetryCount()).thenReturn(1);
		when(retryContext.getServiceInstance()).thenReturn(serviceInstance);
		when(retryPolicy.canRetryNextServer(retryContext)).thenReturn(true);

		PolarisInterceptorRetryPolicy policy = new PolarisInterceptorRetryPolicy(request, retryPolicy,
				ribbonClient, SERVICE_NAME, routerContext);

		assertThat(policy.canRetry(retryContext)).isTrue();

		// delegate to retry policy, no instance selection happens
		verify(ribbonClient, never()).choose(eq(SERVICE_NAME), any());
		verify(retryContext, never()).setServiceInstance(any());
		assertThat(request.getHeaders().containsKey(RouterConstant.HEADER_ROUTER_CONTEXT)).isFalse();
	}

	@Test
	public void testFirstAttemptWithRibbonChooserButInstanceExists() {
		RibbonLoadBalancerClient ribbonClient = mock(RibbonLoadBalancerClient.class);

		LoadBalancedRetryContext retryContext = mock(LoadBalancedRetryContext.class);
		when(retryContext.getRetryCount()).thenReturn(0);
		when(retryContext.getServiceInstance()).thenReturn(serviceInstance);
		when(retryPolicy.canRetryNextServer(retryContext)).thenReturn(false);

		PolarisInterceptorRetryPolicy policy = new PolarisInterceptorRetryPolicy(request, retryPolicy,
				ribbonClient, SERVICE_NAME, routerContext);

		assertThat(policy.canRetry(retryContext)).isFalse();

		verify(ribbonClient, never()).choose(eq(SERVICE_NAME), any());
	}

	@Test
	public void testFirstAttemptWithNonRibbonChooser() {
		ServiceInstanceChooser chooser = mock(ServiceInstanceChooser.class);
		when(chooser.choose(SERVICE_NAME)).thenReturn(serviceInstance);

		LoadBalancedRetryContext retryContext = mock(LoadBalancedRetryContext.class);
		when(retryContext.getRetryCount()).thenReturn(0);
		when(retryContext.getServiceInstance()).thenReturn(null);

		PolarisInterceptorRetryPolicy policy = new PolarisInterceptorRetryPolicy(request, retryPolicy,
				chooser, SERVICE_NAME, routerContext);

		// fall back to parent behavior: choose by service name and return true
		assertThat(policy.canRetry(retryContext)).isTrue();

		verify(chooser).choose(SERVICE_NAME);
		verify(retryContext).setServiceInstance(serviceInstance);
	}

	@Test
	public void testRetryWithNonRibbonChooser() {
		ServiceInstanceChooser chooser = mock(ServiceInstanceChooser.class);

		LoadBalancedRetryContext retryContext = mock(LoadBalancedRetryContext.class);
		when(retryContext.getRetryCount()).thenReturn(1);
		when(retryContext.getServiceInstance()).thenReturn(serviceInstance);
		when(retryPolicy.canRetryNextServer(retryContext)).thenReturn(true);

		PolarisInterceptorRetryPolicy policy = new PolarisInterceptorRetryPolicy(request, retryPolicy,
				chooser, SERVICE_NAME, routerContext);

		assertThat(policy.canRetry(retryContext)).isTrue();

		verify(retryPolicy).canRetryNextServer(retryContext);
	}
}
