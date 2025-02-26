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

package com.tencent.cloud.rpc.enhancement.instrument.webclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.plugin.DefaultEnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.client.api.SDKContext;
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
import reactor.test.StepVerifier;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static com.tencent.cloud.rpc.enhancement.instrument.resttemplate.PolarisLoadBalancerRequestTransformer.LOAD_BALANCER_SERVICE_INSTANCE;
import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnhancedWebClientExchangeFilterFunctionTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	@Mock
	private RpcEnhancementReporterProperties reporterProperties;
	@Mock
	private SDKContext sdkContext;
	@Mock
	private Registration registration;
	@Mock
	private ClientRequest clientRequest;
	@Mock
	private ExchangeFunction exchangeFunction;
	@Mock
	private ClientResponse clientResponse;
	@Mock
	private EnhancedPluginRunner pluginRunner;
	@Mock
	private ServiceInstance serviceInstance;

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
		MetadataContextHolder.get().setLoadbalancer(LOAD_BALANCER_SERVICE_INSTANCE, serviceInstance);
	}

	@Test
	public void testRun() throws URISyntaxException {

		doReturn(new URI("http://0.0.0.0/")).when(clientRequest).url();
		doReturn(new HttpHeaders()).when(clientRequest).headers();
		doReturn(HttpMethod.GET).when(clientRequest).method();
		ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
		doReturn(headers).when(clientResponse).headers();
		doReturn(HttpStatusCode.valueOf(200)).when(clientResponse).statusCode();
		doReturn(Mono.just(clientResponse)).when(exchangeFunction).exchange(any());

		EnhancedWebClientExchangeFilterFunction reporter = new EnhancedWebClientExchangeFilterFunction(new DefaultEnhancedPluginRunner(new ArrayList<>(), registration, null));
		ClientResponse clientResponse1 = reporter.filter(clientRequest, exchangeFunction).block();
		assertThat(clientResponse1).isEqualTo(clientResponse);

		ClientResponse clientResponse2 = reporter.filter(clientRequest, exchangeFunction).block();
		assertThat(clientResponse2).isEqualTo(clientResponse);

		doReturn(Mono.error(new RuntimeException())).when(exchangeFunction).exchange(any());

		assertThatThrownBy(() -> reporter.filter(clientRequest, exchangeFunction)
				.block()).isInstanceOf(RuntimeException.class);

	}

	@Test
	void testCallAbortedWithFallback() {
		// Arrange
		URI uri = URI.create("http://127.0.0.1:8080/api");
		ClientRequest request = ClientRequest.create(HttpMethod.GET, uri).build();

		Map<String, String> headers = new HashMap<>();
		headers.put("header-key", "header-value");

		CircuitBreakerStatus.FallbackInfo fallbackInfo = new CircuitBreakerStatus.FallbackInfo(HttpStatus.SERVICE_UNAVAILABLE.value(), headers, "Fallback Response");

		CallAbortedException abortedException = new CallAbortedException("test rule", fallbackInfo);

		doThrow(abortedException)
				.when(pluginRunner)
				.run(eq(EnhancedPluginType.Client.PRE), any());
		when(serviceInstance.getServiceId()).thenReturn("test-service");

		EnhancedWebClientExchangeFilterFunction filterFunction = new EnhancedWebClientExchangeFilterFunction(pluginRunner);

		// Act
		Mono<ClientResponse> responseMono = filterFunction.filter(request, exchangeFunction);

		// Assert
		StepVerifier.create(responseMono)
				.expectNextMatches(response -> {
					Assertions.assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode());
					assertThat(response.headers().asHttpHeaders().containsKey("header-key")).isTrue();
					return true;
				})
				.verifyComplete();
	}

	@Test
	void testCallAborted() {
		// Arrange
		URI uri = URI.create("http://127.0.0.1:8080/api");
		ClientRequest request = ClientRequest.create(HttpMethod.GET, uri).build();

		CallAbortedException abortedException = new CallAbortedException("test rule", null);

		doThrow(abortedException)
				.when(pluginRunner)
				.run(eq(EnhancedPluginType.Client.PRE), any());
		when(serviceInstance.getServiceId()).thenReturn("test-service");

		EnhancedWebClientExchangeFilterFunction filterFunction = new EnhancedWebClientExchangeFilterFunction(pluginRunner);

		// Act
		assertThatThrownBy(() -> filterFunction.filter(request, exchangeFunction)).
				isInstanceOf(CallAbortedException.class);
	}

}
