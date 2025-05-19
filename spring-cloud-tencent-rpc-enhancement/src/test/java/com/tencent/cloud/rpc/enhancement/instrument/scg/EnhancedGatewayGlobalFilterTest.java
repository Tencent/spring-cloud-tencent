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

package com.tencent.cloud.rpc.enhancement.instrument.scg;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.plugin.DefaultEnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.client.api.SDKContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@ExtendWith(MockitoExtension.class)
public class EnhancedGatewayGlobalFilterTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	@Mock
	Registration registration;
	@Mock
	ServerWebExchange exchange;
	@Mock
	GatewayFilterChain chain;
	@Mock
	ServerHttpResponse response;
	@Mock
	ServerHttpRequest request;
	@Mock
	private RpcEnhancementReporterProperties reporterProperties;
	@Mock
	private SDKContext sdkContext;
	@Mock
	private Response<ServiceInstance> serviceInstanceResponse;
	@Mock
	private ServiceInstance serviceInstance;
	@Mock
	private EnhancedPluginRunner pluginRunner;


	@BeforeAll
	static void beforeAll() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("unit-test");
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		MetadataLocalProperties metadataLocalProperties = mock(MetadataLocalProperties.class);
		StaticMetadataManager staticMetadataManager = mock(StaticMetadataManager.class);
		doReturn(metadataLocalProperties).when(applicationContext).getBean(MetadataLocalProperties.class);
		doReturn(staticMetadataManager).when(applicationContext).getBean(StaticMetadataManager.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext)
				.thenReturn(applicationContext);
	}

	@AfterAll
	static void afterAll() {
		mockedApplicationContextAwareUtils.close();
	}

	@BeforeEach
	void setUp() {
		MetadataContext.LOCAL_NAMESPACE = NAMESPACE_TEST;
		MetadataContext.LOCAL_SERVICE = SERVICE_PROVIDER;
	}

	@Test
	public void testRun() throws URISyntaxException {

		doReturn(new URI("http://0.0.0.0/")).when(request).getURI();
		doReturn(new HttpHeaders()).when(request).getHeaders();
		doReturn(HttpMethod.GET).when(request).getMethod();
		doReturn(new HttpHeaders()).when(response).getHeaders();
		doReturn(Mono.empty()).when(chain).filter(exchange);
		Route route = mock(Route.class);
		URI uri = new URI("http://TEST/");
		doReturn(uri).when(route).getUri();
		doReturn(route).when(exchange).getAttribute(GATEWAY_ROUTE_ATTR);
		doReturn(new URI("http://0.0.0.0/")).when(exchange).getAttribute(GATEWAY_REQUEST_URL_ATTR);
		doReturn(request).when(exchange).getRequest();
		doReturn(response).when(exchange).getResponse();
		doReturn(serviceInstanceResponse).when(exchange).getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR);
		doReturn(serviceInstance).when(serviceInstanceResponse).getServer();
		doReturn("test-service").when(serviceInstance).getServiceId();

		EnhancedGatewayGlobalFilter reporter = new EnhancedGatewayGlobalFilter(new DefaultEnhancedPluginRunner(new ArrayList<>(), registration, null));
		reporter.getOrder();

		reporter.filter(exchange, chain).block();

		doReturn(Mono.error(new RuntimeException())).when(chain).filter(exchange);

		assertThatThrownBy(() -> reporter.filter(exchange, chain).block()).isInstanceOf(RuntimeException.class);

	}

	@Test
	void testFilterWithCallAbortedException() {
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		CallAbortedException exception = new CallAbortedException("Test abort", null);

		when(exchange.getRequest()).thenReturn(request);
		when(request.getHeaders()).thenReturn(headers);
		when(request.getMethod()).thenReturn(HttpMethod.GET);
		doThrow(exception).when(pluginRunner).run(eq(EnhancedPluginType.Client.PRE), any(EnhancedPluginContext.class));

		EnhancedGatewayGlobalFilter filter = new EnhancedGatewayGlobalFilter(pluginRunner);
		// Act & Assert
		Assertions.assertThrows(CallAbortedException.class, () -> {
			filter.filter(exchange, chain);
		});
	}

	@Test
	void testFilterWithCallAbortedExceptionAndFallback() {
		// Arrange
		MultiValueMap<String, String> headers = new TestMultiValueMap();
		headers.put("header-key", Collections.singletonList("header-value"));

		Map<String, String> headersMap = new HashMap<>();
		headersMap.put("header-key", "header-value");

		CircuitBreakerStatus.FallbackInfo fallbackInfo = new CircuitBreakerStatus.FallbackInfo(HttpStatus.INTERNAL_SERVER_ERROR.value(), headersMap, "Fallback Response");

		CallAbortedException abortedException = new CallAbortedException("test rule", fallbackInfo);
		ServerHttpResponse response = mock(ServerHttpResponse.class);
		HttpHeaders responseHeaders = new HttpHeaders();

		when(exchange.getRequest()).thenReturn(request);
		when(request.getHeaders()).thenReturn(new HttpHeaders(headers));
		when(request.getMethod()).thenReturn(HttpMethod.GET);
		when(exchange.getResponse()).thenReturn(response);
		when(response.getHeaders()).thenReturn(responseHeaders);
		when(response.bufferFactory()).thenReturn(new DefaultDataBufferFactory());

		doThrow(abortedException).when(pluginRunner)
				.run(eq(EnhancedPluginType.Client.PRE), any(EnhancedPluginContext.class));
		EnhancedGatewayGlobalFilter filter = new EnhancedGatewayGlobalFilter(pluginRunner);

		// Act
		Mono<Void> result = filter.filter(exchange, chain);

		// Assert
		verify(response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public static class TestMultiValueMap implements MultiValueMap<String, String> {

		@Override
		public String getFirst(String key) {
			return null;
		}

		@Override
		public void add(String key, String value) {

		}

		@Override
		public void addAll(String key, List<? extends String> values) {

		}

		@Override
		public void addAll(MultiValueMap<String, String> values) {

		}

		@Override
		public void set(String key, String value) {

		}

		@Override
		public void setAll(Map<String, String> values) {

		}

		@Override
		public Map<String, String> toSingleValueMap() {
			return null;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean containsKey(Object key) {
			return false;
		}

		@Override
		public boolean containsValue(Object value) {
			return false;
		}

		@Override
		public List<String> get(Object key) {
			return null;
		}

		@Nullable
		@Override
		public List<String> put(String key, List<String> value) {
			return null;
		}

		@Override
		public List<String> remove(Object key) {
			return null;
		}

		@Override
		public void putAll(@NotNull Map<? extends String, ? extends List<String>> m) {

		}

		@Override
		public void clear() {

		}

		@NotNull
		@Override
		public Set<String> keySet() {
			return null;
		}

		@NotNull
		@Override
		public Collection<List<String>> values() {
			return null;
		}

		@NotNull
		@Override
		public Set<Entry<String, List<String>>> entrySet() {
			return null;
		}

		@Override
		public boolean equals(Object o) {
			return false;
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}
}
