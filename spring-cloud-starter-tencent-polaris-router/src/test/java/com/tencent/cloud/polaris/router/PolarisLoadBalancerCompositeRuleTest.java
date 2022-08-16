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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.AvailabilityFilteringRule;
import com.netflix.loadbalancer.BestAvailableRule;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.RetryRule;
import com.netflix.loadbalancer.RoundRobinRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.WeightedResponseTimeRule;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.pojo.PolarisServer;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.loadbalancer.PolarisWeightedRule;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisMetadataRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisNearByRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisRuleBasedRouterProperties;
import com.tencent.cloud.polaris.router.interceptor.MetadataRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.NearbyRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.interceptor.RuleBasedRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * test for {@link PolarisLoadBalancerCompositeRule}.
 *
 * @author lepdou 2022-05-26
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisLoadBalancerCompositeRuleTest {

	private static final AtomicBoolean initTransitiveMetadata = new AtomicBoolean(false);
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

	private IClientConfig config;
	private String testNamespace = "testNamespace";
	private String testCallerService = "testCallerService";
	private String testCalleeService = "testCalleeService";

	private final List<RouterRequestInterceptor> requestInterceptors = new ArrayList<>();

	@Before
	public void before() {
		config = new DefaultClientConfigImpl();
		config.loadDefaultValues();
		requestInterceptors.add(new MetadataRouterRequestInterceptor(polarisMetadataRouterProperties));
		requestInterceptors.add(new NearbyRouterRequestInterceptor(polarisNearByRouterProperties));
		requestInterceptors.add(new RuleBasedRouterRequestInterceptor(polarisRuleBasedRouterProperties));
	}

	@Test
	public void testGetDefaultLB() {
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn("");
		PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
				polarisLoadBalancerProperties, config, requestInterceptors, null, null);

		AbstractLoadBalancerRule defaultRule = compositeRule.getRule();

		Assert.assertNull(defaultRule);
	}

	@Test
	public void testRandomLB() {
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn(PolarisLoadBalancerCompositeRule.STRATEGY_RANDOM);
		PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
				polarisLoadBalancerProperties, config, requestInterceptors, null, null);

		AbstractLoadBalancerRule lbRule = compositeRule.getRule();

		Assert.assertTrue(lbRule instanceof RandomRule);
	}

	@Test
	public void testWeightLB() {
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn(PolarisLoadBalancerCompositeRule.STRATEGY_WEIGHT);
		PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
				polarisLoadBalancerProperties, config, requestInterceptors, null, null);

		AbstractLoadBalancerRule lbRule = compositeRule.getRule();

		Assert.assertTrue(lbRule instanceof PolarisWeightedRule);
	}

	@Test
	public void testRetryLB() {
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn(PolarisLoadBalancerCompositeRule.STRATEGY_RETRY);
		PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
				polarisLoadBalancerProperties, config, requestInterceptors, null, null);

		AbstractLoadBalancerRule lbRule = compositeRule.getRule();

		Assert.assertTrue(lbRule instanceof RetryRule);
	}

	@Test
	public void testWeightedResponseTimeLB() {
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn(PolarisLoadBalancerCompositeRule.STRATEGY_RESPONSE_TIME_WEIGHTED);
		PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
				polarisLoadBalancerProperties, config, requestInterceptors, null, null);

		AbstractLoadBalancerRule lbRule = compositeRule.getRule();

		Assert.assertTrue(lbRule instanceof WeightedResponseTimeRule);
	}

	@Test
	public void tesBestAvailableLB() {
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn(PolarisLoadBalancerCompositeRule.STRATEGY_BEST_AVAILABLE);
		PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
				polarisLoadBalancerProperties, config, requestInterceptors, null, null);

		AbstractLoadBalancerRule lbRule = compositeRule.getRule();

		Assert.assertTrue(lbRule instanceof BestAvailableRule);
	}

	@Test
	public void tesRoundRobinLB() {
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn(PolarisLoadBalancerCompositeRule.STRATEGY_ROUND_ROBIN);
		PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
				polarisLoadBalancerProperties, config, requestInterceptors, null, null);

		AbstractLoadBalancerRule lbRule = compositeRule.getRule();

		Assert.assertTrue(lbRule instanceof RoundRobinRule);
	}

	@Test
	public void testAvailabilityFilteringLB() {
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn(PolarisLoadBalancerCompositeRule.STRATEGY_AVAILABILITY_FILTERING);
		PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
				polarisLoadBalancerProperties, config, requestInterceptors, null, null);

		AbstractLoadBalancerRule lbRule = compositeRule.getRule();

		Assert.assertTrue(lbRule instanceof AvailabilityFilteringRule);
	}

	@Test
	public void testBuildMetadataRouteRequest() {
		when(polarisMetadataRouterProperties.isEnabled()).thenReturn(true);
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn("");

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
					polarisLoadBalancerProperties, config, requestInterceptors, null, null);

			ServiceInstances serviceInstances = assembleServiceInstances();
			PolarisRouterContext routerContext = assembleRouterContext();

			Map<String, String> oldRouterLabels = routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS);
			Map<String, String> newRouterLabels = new HashMap<>(oldRouterLabels);
			newRouterLabels.put("system-metadata-router-keys", "k2");
			routerContext.putLabels(PolarisRouterContext.ROUTER_LABELS, newRouterLabels);

			ProcessRoutersRequest request = compositeRule.buildProcessRoutersBaseRequest(serviceInstances);
			compositeRule.processRouterRequestInterceptors(request, routerContext);

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
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn("");

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
					polarisLoadBalancerProperties, config, requestInterceptors, null, null);

			ServiceInstances serviceInstances = assembleServiceInstances();
			PolarisRouterContext routerContext = assembleRouterContext();

			ProcessRoutersRequest request = compositeRule.buildProcessRoutersBaseRequest(serviceInstances);
			compositeRule.processRouterRequestInterceptors(request, routerContext);

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
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn("");

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString())).
					thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
					polarisLoadBalancerProperties, config, requestInterceptors, null, null);

			ServiceInstances serviceInstances = assembleServiceInstances();
			PolarisRouterContext routerContext = assembleRouterContext();

			ProcessRoutersRequest request = compositeRule.buildProcessRoutersBaseRequest(serviceInstances);
			compositeRule.processRouterRequestInterceptors(request, routerContext);

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
		when(polarisLoadBalancerProperties.getStrategy()).thenReturn("");

		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testCallerService);

			setTransitiveMetadata();

			PolarisLoadBalancerCompositeRule compositeRule = new PolarisLoadBalancerCompositeRule(routerAPI,
					polarisLoadBalancerProperties, config, requestInterceptors, null, null);

			ProcessRoutersResponse assembleResponse = assembleProcessRoutersResponse();
			when(routerAPI.processRouters(any())).thenReturn(assembleResponse);

			List<Server> servers = compositeRule.doRouter(assembleServers(), assembleRouterContext());

			Assert.assertEquals(assembleResponse.getServiceInstances().getInstances().size(), servers.size());
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
		routerContext.putLabels(PolarisRouterContext.TRANSITIVE_LABELS, transitiveLabels);
		routerContext.putLabels(PolarisRouterContext.ROUTER_LABELS, routerLabels);
		return routerContext;
	}

	private ProcessRoutersResponse assembleProcessRoutersResponse() {
		return new ProcessRoutersResponse(assembleServiceInstances());
	}

	private List<Server> assembleServers() {
		ServiceInstances serviceInstances = assembleServiceInstances();
		List<Server> servers = new LinkedList<>();
		servers.add(new PolarisServer(serviceInstances, new DefaultInstance()));
		servers.add(new PolarisServer(serviceInstances, new DefaultInstance()));
		servers.add(new PolarisServer(serviceInstances, new DefaultInstance()));
		servers.add(new PolarisServer(serviceInstances, new DefaultInstance()));
		return servers;
	}
}
