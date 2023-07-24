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

package com.tencent.cloud.rpc.enhancement.scg;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.plugin.DefaultEnhancedPluginRunner;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.jupiter.api.AfterAll;
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
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

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

		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		Response<ServiceInstance> serviceInstanceResponse = new Response<ServiceInstance>() {
			@Override
			public boolean hasServer() {
				return true;
			}

			@Override
			public ServiceInstance getServer() {
				return serviceInstance;
			}
		};
		doReturn(serviceInstanceResponse).when(exchange).getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR);
		doReturn(new URI("http://0.0.0.0/")).when(exchange).getAttribute(GATEWAY_REQUEST_URL_ATTR);
		doReturn(request).when(exchange).getRequest();
		doReturn(response).when(exchange).getResponse();

		EnhancedGatewayGlobalFilter reporter = new EnhancedGatewayGlobalFilter(new DefaultEnhancedPluginRunner(new ArrayList<>(), registration, null));
		reporter.getOrder();

		reporter.filter(exchange, chain).block();

		doReturn(Mono.error(new RuntimeException())).when(chain).filter(exchange);

		assertThatThrownBy(() -> reporter.filter(exchange, chain).block()).isInstanceOf(RuntimeException.class);

	}
}
