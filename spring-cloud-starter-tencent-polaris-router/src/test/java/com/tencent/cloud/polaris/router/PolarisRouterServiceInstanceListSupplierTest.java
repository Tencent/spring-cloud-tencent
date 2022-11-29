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

package com.tencent.cloud.polaris.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.pojo.PolarisServiceInstance;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisMetadataRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisNearByRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisRuleBasedRouterProperties;
import com.tencent.cloud.polaris.router.interceptor.MetadataRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.NearbyRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.RuleBasedRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.plugins.router.metadata.MetadataRouter;
import com.tencent.polaris.plugins.router.nearby.NearbyRouter;
import com.tencent.polaris.plugins.router.rule.RuleBasedRouter;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * test for {@link PolarisRouterServiceInstanceListSupplier}.
 *
 * @author lepdou 2022-05-26
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisRouterServiceInstanceListSupplierTest {

	private static final AtomicBoolean initTransitiveMetadata = new AtomicBoolean(false);
	private final String testNamespace = "testNamespace";
	private final String testCallerService = "testCallerService";
	private final String testCalleeService = "testCalleeService";
	private final List<RouterRequestInterceptor> requestInterceptors = new ArrayList<>();
	@Mock
	private ServiceInstanceListSupplier delegate;
	@Mock
	private PolarisLoadBalancerProperties polarisLoadBalancerProperties;
	@Mock
	private PolarisNearByRouterProperties polarisNearByRouterProperties;
	@Mock
	private PolarisMetadataRouterProperties polarisMetadataRouterProperties;
	@Mock
	private PolarisRuleBasedRouterProperties polarisRuleBasedRouterProperties;
	@Mock
	private RouterAPI routerAPI;

	@Before
	public void before() {
		requestInterceptors.add(new MetadataRouterRequestInterceptor(polarisMetadataRouterProperties));
		requestInterceptors.add(new NearbyRouterRequestInterceptor(polarisNearByRouterProperties));
		requestInterceptors.add(new RuleBasedRouterRequestInterceptor(polarisRuleBasedRouterProperties));
	}

	@Test
	public void testBuildMetadataRouteRequest() {
		when(polarisMetadataRouterProperties.isEnabled()).thenReturn(true);

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisRouterServiceInstanceListSupplier polarisSupplier = new PolarisRouterServiceInstanceListSupplier(
					delegate, routerAPI, requestInterceptors, null);

			ServiceInstances serviceInstances = assembleServiceInstances();
			PolarisRouterContext routerContext = assembleRouterContext();

			Map<String, String> oldRouterLabels = routerContext.getLabels(RouterConstant.ROUTER_LABELS);
			Map<String, String> newRouterLabels = new HashMap<>(oldRouterLabels);
			newRouterLabels.put("system-metadata-router-keys", "k2");
			routerContext.putLabels(RouterConstant.ROUTER_LABELS, newRouterLabels);

			ProcessRoutersRequest request = polarisSupplier.buildProcessRoutersRequest(serviceInstances, routerContext);
			polarisSupplier.processRouterRequestInterceptors(request, routerContext);

			Set<RouteArgument> routerMetadata = request.getRouterArguments(MetadataRouter.ROUTER_TYPE_METADATA);

			Assert.assertEquals(1, routerMetadata.size());
			Assert.assertEquals(0, request.getRouterArguments(NearbyRouter.ROUTER_TYPE_NEAR_BY).size());
			Assert.assertEquals(1, request.getRouterArguments(RuleBasedRouter.ROUTER_TYPE_RULE_BASED).size());

			for (RouteArgument routeArgument : request.getRouterArguments(RuleBasedRouter.ROUTER_TYPE_RULE_BASED)) {
				Assert.assertEquals(RuleBasedRouter.ROUTER_ENABLED, routeArgument.getKey());
				Assert.assertEquals("false", routeArgument.getValue());
			}
		}
	}

	@Test
	public void testBuildNearbyRouteRequest() {
		when(polarisNearByRouterProperties.isEnabled()).thenReturn(true);

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisRouterServiceInstanceListSupplier polarisSupplier = new PolarisRouterServiceInstanceListSupplier(
					delegate, routerAPI, requestInterceptors, null);

			ServiceInstances serviceInstances = assembleServiceInstances();
			PolarisRouterContext routerContext = assembleRouterContext();

			ProcessRoutersRequest request = polarisSupplier.buildProcessRoutersRequest(serviceInstances, routerContext);
			polarisSupplier.processRouterRequestInterceptors(request, routerContext);

			Set<RouteArgument> routerMetadata = request.getRouterArguments(NearbyRouter.ROUTER_TYPE_NEAR_BY);

			Assert.assertEquals(0, request.getRouterArguments(MetadataRouter.ROUTER_TYPE_METADATA).size());
			Assert.assertEquals(1, routerMetadata.size());

			for (RouteArgument routeArgument : routerMetadata) {
				Assert.assertEquals(NearbyRouter.ROUTER_ENABLED, routeArgument.getKey());
				Assert.assertEquals("true", routeArgument.getValue());
			}

			Assert.assertEquals(1, request.getRouterArguments(RuleBasedRouter.ROUTER_TYPE_RULE_BASED).size());

			for (RouteArgument routeArgument : request.getRouterArguments(RuleBasedRouter.ROUTER_TYPE_RULE_BASED)) {
				Assert.assertEquals(RuleBasedRouter.ROUTER_ENABLED, routeArgument.getKey());
				Assert.assertEquals("false", routeArgument.getValue());
			}
		}
	}

	@Test
	public void testBuildRuleBasedRouteRequest() {
		when(polarisRuleBasedRouterProperties.isEnabled()).thenReturn(true);

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString())).
					thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisRouterServiceInstanceListSupplier polarisSupplier = new PolarisRouterServiceInstanceListSupplier(
					delegate, routerAPI, requestInterceptors, null);

			ServiceInstances serviceInstances = assembleServiceInstances();
			PolarisRouterContext routerContext = assembleRouterContext();

			ProcessRoutersRequest request = polarisSupplier.buildProcessRoutersRequest(serviceInstances, routerContext);
			polarisSupplier.processRouterRequestInterceptors(request, routerContext);

			Set<RouteArgument> routerMetadata = request.getRouterArguments(RuleBasedRouter.ROUTER_TYPE_RULE_BASED);

			Assert.assertEquals(3, routerMetadata.size());
			Assert.assertEquals(0, request.getRouterArguments(MetadataRouter.ROUTER_TYPE_METADATA).size());
			Assert.assertEquals(0, request.getRouterArguments(NearbyRouter.ROUTER_TYPE_NEAR_BY).size());
		}
	}

	@Test
	public void testRouter() {
		when(polarisRuleBasedRouterProperties.isEnabled()).thenReturn(true);

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisRouterServiceInstanceListSupplier polarisSupplier = new PolarisRouterServiceInstanceListSupplier(
					delegate, routerAPI, requestInterceptors, null);

			ProcessRoutersResponse assembleResponse = assembleProcessRoutersResponse();
			when(routerAPI.processRouters(any())).thenReturn(assembleResponse);

			Flux<List<ServiceInstance>> servers = polarisSupplier.doRouter(assembleServers(), assembleRouterContext());


			Assert.assertEquals(assembleResponse.getServiceInstances().getInstances().size(),
					servers.toStream().mapToLong(List::size).sum());
		}
	}

	@Test
	public void buildRouterContext() {
		PolarisRouterServiceInstanceListSupplier polarisSupplier = new PolarisRouterServiceInstanceListSupplier(
				delegate, routerAPI, requestInterceptors, null);

		HttpHeaders headers = new HttpHeaders();
		PolarisRouterContext context = polarisSupplier.buildRouterContext(headers);
		Assert.assertNull(context);

		// mock
		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString())).thenReturn("mock-value");
			MetadataContextHolder.set(new MetadataContext());

			headers = new HttpHeaders();
			headers.add(RouterConstant.ROUTER_LABEL_HEADER, "{\"k1\":\"v1\"}");
			PolarisRouterContext routerContext = polarisSupplier.buildRouterContext(headers);
			assertThat(routerContext.getLabel("k1")).isEqualTo("v1");
		}
	}

	@Test
	public void testGet01() {
		PolarisRouterServiceInstanceListSupplier polarisSupplier = new PolarisRouterServiceInstanceListSupplier(
				delegate, routerAPI, requestInterceptors, null);
		Assertions.assertThrows(PolarisException.class, () -> polarisSupplier.get());
	}

	@Test
	public void testGet02() {
		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testCallerService);

			PolarisRouterServiceInstanceListSupplier polarisSupplier = new PolarisRouterServiceInstanceListSupplier(
					delegate, routerAPI, requestInterceptors, null);

			MockServerHttpRequest httpRequest = MockServerHttpRequest.get("/" + testCalleeService + "/users")
					.header("k1", "v1")
					.queryParam("userid", "zhangsan")
					.build();
			RequestDataContext requestDataContext = new RequestDataContext(new RequestData(httpRequest), "blue");
			DefaultRequest request = new DefaultRequest(requestDataContext);
			Assert.assertNull(polarisSupplier.get(request));
		}
	}

	private void setTransitiveMetadata() {
		if (initTransitiveMetadata.compareAndSet(false, true)) {
			// mock transitive metadata
			MetadataContext metadataContext = Mockito.mock(MetadataContext.class);
			try (MockedStatic<MetadataContextHolder> mockedMetadataContextHolder = Mockito.mockStatic(MetadataContextHolder.class)) {
				mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);
			}
		}
	}

	private ServiceInstances assembleServiceInstances() {
		ServiceKey serviceKey = new ServiceKey(testNamespace, testCalleeService);
		List<Instance> instances = new LinkedList<>();
		instances.add(new DefaultInstance());
		instances.add(new DefaultInstance());
		instances.add(new DefaultInstance());
		instances.add(new DefaultInstance());
		instances.add(new DefaultInstance());

		return new DefaultServiceInstances(serviceKey, instances);
	}

	private PolarisRouterContext assembleRouterContext() {
		PolarisRouterContext routerContext = new PolarisRouterContext();
		Map<String, String> transitiveLabels = new HashMap<>();
		transitiveLabels.put("k1", "v1");
		Map<String, String> routerLabels = new HashMap<>();
		routerLabels.put("k2", "v2");
		routerLabels.put("k3", "v3");
		routerContext.putLabels(RouterConstant.TRANSITIVE_LABELS, transitiveLabels);
		routerContext.putLabels(RouterConstant.ROUTER_LABELS, routerLabels);
		return routerContext;
	}

	private ProcessRoutersResponse assembleProcessRoutersResponse() {
		return new ProcessRoutersResponse(assembleServiceInstances());
	}

	private Flux<List<ServiceInstance>> assembleServers() {
		ServiceInstances serviceInstances = assembleServiceInstances();
		List<ServiceInstance> servers = new ArrayList<>();
		for (Instance instance : serviceInstances.getInstances()) {
			servers.add(new PolarisServiceInstance(instance));
		}
		return Flux.fromIterable(Collections.singletonList(servers));
	}
}
