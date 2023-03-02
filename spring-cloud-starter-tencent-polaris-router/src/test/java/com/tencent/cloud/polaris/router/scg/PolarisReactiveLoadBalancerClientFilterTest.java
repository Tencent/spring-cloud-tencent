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

package com.tencent.cloud.polaris.router.scg;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR;

/**
 * Test for ${@link PolarisReactiveLoadBalancerClientFilter}.
 *
 * @author lepdou 2022-07-04
 */
@ExtendWith(MockitoExtension.class)
public class PolarisReactiveLoadBalancerClientFilterTest {

	private static final String testNamespaceAndService = "testNamespaceAndService";
	private static final String calleeService = "calleeService";

	@Mock
	private StaticMetadataManager staticMetadataManager;
	@Mock
	private SpringWebRouterLabelResolver routerLabelResolver;
	@Mock
	private RouterRuleLabelResolver routerRuleLabelResolver;
	@Mock
	private LoadBalancerClientFactory loadBalancerClientFactory;
	@Mock
	private GatewayLoadBalancerProperties gatewayLoadBalancerProperties;
	@Mock
	private PolarisContextProperties polarisContextProperties;

	@Test
	public void testGenRouterHttpHeaders() throws UnsupportedEncodingException {
		try (
				MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = mockStatic(ApplicationContextAwareUtils.class);
				MockedStatic<MetadataContextHolder> mockedMetadataContextHolder = mockStatic(MetadataContextHolder.class)
		) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testNamespaceAndService);

			MetadataContext metadataContext = mock(MetadataContext.class);

			// mock transitive metadata
			Map<String, String> transitiveLabels = new HashMap<>();
			transitiveLabels.put("t1", "v1");
			transitiveLabels.put("t2", "v2");
			when(metadataContext.getTransitiveMetadata()).thenReturn(transitiveLabels);
			mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);

			PolarisReactiveLoadBalancerClientFilter filter = new PolarisReactiveLoadBalancerClientFilter(loadBalancerClientFactory,
					gatewayLoadBalancerProperties, staticMetadataManager, routerRuleLabelResolver,
					Collections.singletonList(routerLabelResolver), polarisContextProperties);

			Map<String, String> localMetadata = new HashMap<>();
			localMetadata.put("env", "blue");
			when(staticMetadataManager.getMergedStaticMetadata()).thenReturn(localMetadata);

			Set<String> expressionLabelKeys = Sets.set("${http.header.k1}", "${http.query.userid}");
			when(routerRuleLabelResolver.getExpressionLabelKeys(anyString(), anyString(), anyString())).thenReturn(expressionLabelKeys);

			MockServerHttpRequest request = MockServerHttpRequest.get("/" + calleeService + "/users")
					.header("k1", "v1")
					.queryParam("userid", "zhangsan")
					.build();
			MockServerWebExchange webExchange = new MockServerWebExchange.Builder(request).build();

			Map<String, String> customMetadata = new HashMap<>();
			customMetadata.put("k2", "v2");
			when(routerLabelResolver.resolve(webExchange, expressionLabelKeys)).thenReturn(customMetadata);

			HttpHeaders headers = filter.genRouterHttpHeaders(webExchange, calleeService);

			assertThat(headers).isNotNull();
			List<String> routerHeaders = headers.get(RouterConstant.ROUTER_LABEL_HEADER);
			assertThat(routerHeaders).isNotNull();


			Map<String, String> routerLabels = JacksonUtils.deserialize2Map(URLDecoder.decode(routerHeaders.get(0), UTF_8));
			assertThat(routerLabels.get("${http.header.k1}")).isEqualTo("v1");
			assertThat(routerLabels.get("${http.query.userid}")).isEqualTo("zhangsan");
			assertThat(routerLabels.get("env")).isEqualTo("blue");
			assertThat(routerLabels.get("t1")).isEqualTo("v1");
			assertThat(routerLabels.get("t2")).isEqualTo("v2");
		}
	}

	@Test
	public void testFilter01() throws Exception {
		try (
				MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = mockStatic(ApplicationContextAwareUtils.class);
				MockedStatic<MetadataContextHolder> mockedMetadataContextHolder = mockStatic(MetadataContextHolder.class)
		) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testNamespaceAndService);

			MetadataContext metadataContext = mock(MetadataContext.class);

			// mock transitive metadata
			Map<String, String> transitiveLabels = new HashMap<>();
			transitiveLabels.put("t1", "v1");
			transitiveLabels.put("t2", "v2");
			when(metadataContext.getTransitiveMetadata()).thenReturn(transitiveLabels);
			mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);

			PolarisReactiveLoadBalancerClientFilter filter = new PolarisReactiveLoadBalancerClientFilter(loadBalancerClientFactory,
					gatewayLoadBalancerProperties, staticMetadataManager, routerRuleLabelResolver,
					Lists.list(routerLabelResolver), polarisContextProperties);

			MockServerHttpRequest request = MockServerHttpRequest.get("/" + calleeService + "/users").build();
			MockServerWebExchange exchange = new MockServerWebExchange.Builder(request).build();

			// mock no lb
			EmptyGatewayFilterChain chain = new EmptyGatewayFilterChain();
			Mono<Void> ret = filter.filter(exchange, chain);
			assertThat(ret).isEqualTo(Mono.empty());

			// mock with lb
			exchange = new MockServerWebExchange.Builder(request).build();
			exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, new URI("https://" + calleeService + ":8091"));
			exchange.getAttributes().put(GATEWAY_SCHEME_PREFIX_ATTR, "lb");

			NoopServiceInstanceListSupplier serviceInstanceListSupplier = new NoopServiceInstanceListSupplier();
			RoundRobinLoadBalancer roundRobinLoadBalancer = new RoundRobinLoadBalancer(new SimpleObjectProvider<>(serviceInstanceListSupplier), calleeService);

			when(loadBalancerClientFactory.getInstance(calleeService, ReactorServiceInstanceLoadBalancer.class)).thenReturn(roundRobinLoadBalancer);
			LoadBalancerProperties loadBalancerProperties = mock(LoadBalancerProperties.class);
			when(loadBalancerProperties.getHint()).thenReturn(new HashMap<>());
			when(loadBalancerClientFactory.getProperties(calleeService)).thenReturn(loadBalancerProperties);
			filter.filter(exchange, chain);
		}
	}

	static class EmptyGatewayFilterChain implements GatewayFilterChain {

		@Override
		public Mono<Void> filter(ServerWebExchange exchange) {
			return Mono.empty();
		}
	}
}
