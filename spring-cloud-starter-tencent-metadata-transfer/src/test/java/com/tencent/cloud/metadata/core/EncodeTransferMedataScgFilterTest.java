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

package com.tencent.cloud.metadata.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.plugin.DefaultEnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.scg.EnhancedGatewayGlobalFilter;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;
import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * Test for {@link EncodeTransferMedataScgEnhancedPlugin}.
 * @author quan, Shedfree Wu
 */
@ExtendWith(MockitoExtension.class)
public class EncodeTransferMedataScgFilterTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	@Mock
	Registration registration;
	@Mock
	GatewayFilterChain chain;

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

		Route route = mock(Route.class);
		URI uri = new URI("http://TEST/");
		doReturn(uri).when(route).getUri();

		MetadataContext metadataContext = MetadataContextHolder.get();
		metadataContext.setTransitiveMetadata(Maps.newHashMap("t-key", "t-value"));
		metadataContext.setDisposableMetadata(Maps.newHashMap("d-key", "d-value"));

		MockServerHttpRequest mockServerHttpRequest = MockServerHttpRequest.get("/test").build();

		EncodeTransferMedataScgEnhancedPlugin plugin = new EncodeTransferMedataScgEnhancedPlugin();
		plugin.getOrder();
		EnhancedGatewayGlobalFilter filter = new EnhancedGatewayGlobalFilter(new DefaultEnhancedPluginRunner(Arrays.asList(plugin), registration, null));
		filter.getOrder();

		MockServerWebExchange mockServerWebExchange = MockServerWebExchange.builder(mockServerHttpRequest).build();
		mockServerWebExchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);
		mockServerWebExchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, new URI("http://0.0.0.0/"));
		mockServerWebExchange.getAttributes().put(MetadataConstant.HeaderName.METADATA_CONTEXT, metadataContext);
		doReturn(Mono.empty()).when(chain).filter(any());


		filter.filter(mockServerWebExchange, chain).block();


		ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
		// capture the result exchange
		Mockito.verify(chain).filter(captor.capture());
		ServerWebExchange filteredExchange = captor.getValue();

		assertThat(filteredExchange.getRequest().getHeaders().get(CUSTOM_METADATA)).isNotNull();
		assertThat(filteredExchange.getRequest().getHeaders().get(CUSTOM_DISPOSABLE_METADATA)).isNotNull();

		// test metadataContext init in EnhancedPlugin
		mockServerWebExchange.getAttributes().remove(MetadataConstant.HeaderName.METADATA_CONTEXT);
		assertThatCode(() -> filter.filter(mockServerWebExchange, chain).block()).doesNotThrowAnyException();

	}
}
