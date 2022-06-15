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
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.pojo.PolarisServiceInstance;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.cloud.polaris.router.config.PolarisMetadataRouterProperties;
import com.tencent.cloud.polaris.router.config.PolarisNearByRouterProperties;
import com.tencent.cloud.polaris.router.config.PolarisRuleBasedRouterProperties;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.plugins.router.metadata.MetadataRouter;
import com.tencent.polaris.plugins.router.nearby.NearbyRouter;
import com.tencent.polaris.plugins.router.rule.RuleBasedRouter;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * test for {@link PolarisRouterServiceInstanceListSupplier}
 *@author lepdou 2022-05-26
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisRouterServiceInstanceListSupplierTest {

	private static AtomicBoolean initTransitiveMetadata = new AtomicBoolean(false);
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
	private String testNamespace = "testNamespace";
	private String testCallerService = "testCallerService";
	private String testCalleeService = "testCalleeService";

	@Test
	public void testBuildMetadataRouteRequest() {
		when(polarisMetadataRouterProperties.isEnabled()).thenReturn(true);

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisRouterServiceInstanceListSupplier compositeRule = new PolarisRouterServiceInstanceListSupplier(
					delegate, routerAPI, polarisNearByRouterProperties,
					polarisMetadataRouterProperties, polarisRuleBasedRouterProperties);

			ServiceInstances serviceInstances = assembleServiceInstances();
			PolarisRouterContext routerContext = assembleRouterContext();

			ProcessRoutersRequest request = compositeRule.buildProcessRoutersRequest(serviceInstances, routerContext);

			Map<String, String> routerMetadata = request.getRouterMetadata(MetadataRouter.ROUTER_TYPE_METADATA);

			Assert.assertEquals(1, routerMetadata.size());
			Assert.assertEquals(0, request.getRouterMetadata(NearbyRouter.ROUTER_TYPE_NEAR_BY).size());
			Assert.assertEquals(1, request.getRouterMetadata(RuleBasedRouter.ROUTER_TYPE_RULE_BASED).size());
			Assert.assertEquals("false", request.getRouterMetadata(RuleBasedRouter.ROUTER_TYPE_RULE_BASED)
					.get(RuleBasedRouter.ROUTER_ENABLED));
		}
	}

	@Test
	public void testBuildNearbyRouteRequest() {
		when(polarisNearByRouterProperties.isEnabled()).thenReturn(true);

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisRouterServiceInstanceListSupplier compositeRule = new PolarisRouterServiceInstanceListSupplier(
					delegate, routerAPI, polarisNearByRouterProperties,
					polarisMetadataRouterProperties, polarisRuleBasedRouterProperties);

			ServiceInstances serviceInstances = assembleServiceInstances();
			PolarisRouterContext routerContext = assembleRouterContext();

			ProcessRoutersRequest request = compositeRule.buildProcessRoutersRequest(serviceInstances, routerContext);

			Map<String, String> routerMetadata = request.getRouterMetadata(NearbyRouter.ROUTER_TYPE_NEAR_BY);

			Assert.assertEquals(0, request.getRouterMetadata(MetadataRouter.ROUTER_TYPE_METADATA).size());
			Assert.assertEquals(1, routerMetadata.size());
			Assert.assertEquals("true", routerMetadata.get(NearbyRouter.ROUTER_ENABLED));
			Assert.assertEquals(1, request.getRouterMetadata(RuleBasedRouter.ROUTER_TYPE_RULE_BASED).size());
			Assert.assertEquals("false", request.getRouterMetadata(RuleBasedRouter.ROUTER_TYPE_RULE_BASED)
					.get(RuleBasedRouter.ROUTER_ENABLED));
		}
	}

	@Test
	public void testBuildRuleBasedRouteRequest() {
		when(polarisRuleBasedRouterProperties.isEnabled()).thenReturn(true);

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString())).
					thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisRouterServiceInstanceListSupplier compositeRule = new PolarisRouterServiceInstanceListSupplier(
					delegate, routerAPI, polarisNearByRouterProperties,
					polarisMetadataRouterProperties, polarisRuleBasedRouterProperties);

			ServiceInstances serviceInstances = assembleServiceInstances();
			PolarisRouterContext routerContext = assembleRouterContext();

			ProcessRoutersRequest request = compositeRule.buildProcessRoutersRequest(serviceInstances, routerContext);

			Map<String, String> routerMetadata = request.getRouterMetadata(RuleBasedRouter.ROUTER_TYPE_RULE_BASED);

			Assert.assertEquals(1, routerMetadata.size());
			Assert.assertEquals(0, request.getRouterMetadata(MetadataRouter.ROUTER_TYPE_METADATA).size());
			Assert.assertEquals(0, request.getRouterMetadata(NearbyRouter.ROUTER_TYPE_NEAR_BY).size());
			Assert.assertEquals(1, request.getRouterMetadata(RuleBasedRouter.ROUTER_TYPE_RULE_BASED).size());
			Assert.assertEquals("true", request.getRouterMetadata(RuleBasedRouter.ROUTER_TYPE_RULE_BASED)
					.get(RuleBasedRouter.ROUTER_ENABLED));
		}
	}

	@Test
	public void testRouter() {
		when(polarisRuleBasedRouterProperties.isEnabled()).thenReturn(true);

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisRouterServiceInstanceListSupplier compositeRule = new PolarisRouterServiceInstanceListSupplier(
					delegate, routerAPI, polarisNearByRouterProperties,
					polarisMetadataRouterProperties, polarisRuleBasedRouterProperties);

			ProcessRoutersResponse assembleResponse = assembleProcessRoutersResponse();
			when(routerAPI.processRouters(any())).thenReturn(assembleResponse);

			Flux<List<ServiceInstance>> servers = compositeRule.doRouter(assembleServers(), assembleRouterContext());


			Assert.assertEquals(assembleResponse.getServiceInstances().getInstances().size(),
					servers.toStream().mapToLong(List::size).sum());
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
		routerContext.setLabels(PolarisRouterContext.TRANSITIVE_LABELS, transitiveLabels);
		routerContext.setLabels(PolarisRouterContext.RULE_ROUTER_LABELS, routerLabels);
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
