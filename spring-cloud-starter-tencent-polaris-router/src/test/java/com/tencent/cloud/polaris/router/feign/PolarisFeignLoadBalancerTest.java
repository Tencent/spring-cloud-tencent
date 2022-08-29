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

package com.tencent.cloud.polaris.router.feign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.ILoadBalancer;
import com.tencent.cloud.common.constant.RouterConstants;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.SimpleLoadBalancer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;

import static org.mockito.Mockito.anyString;

/**
 * test for {@link PolarisFeignLoadBalancer}.
 *
 * @author lepdou 2022-05-26
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisFeignLoadBalancerTest {

	@Test
	public void testHasRouterContext() {
		DefaultClientConfigImpl config = new DefaultClientConfigImpl();
		config.loadDefaultValues();
		ILoadBalancer loadBalancer = new SimpleLoadBalancer();
		ServerIntrospector serverIntrospector = new DefaultServerIntrospector();

		PolarisFeignLoadBalancer polarisFeignLoadBalancer = new PolarisFeignLoadBalancer(loadBalancer, config, serverIntrospector);

		Map<String, String> labels = new HashMap<>();
		labels.put("k1", "v1");
		labels.put("k2", "v2");

		List<String> headerValues = new ArrayList<>();
		headerValues.add(JacksonUtils.serialize2Json(labels));

		Map<String, Collection<String>> headers = new HashMap<>();
		headers.put(RouterConstants.ROUTER_LABEL_HEADER, headerValues);

		// mock ApplicationContextAwareUtils#getProperties
		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn("unit-test");

			MetadataContext metadataContext = Mockito.mock(MetadataContext.class);
			// mock MetadataContextHolder#get
			try (MockedStatic<MetadataContextHolder> mockedMetadataContextHolder = Mockito.mockStatic(MetadataContextHolder.class)) {
				mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);

				PolarisRouterContext routerContext = polarisFeignLoadBalancer.buildRouterContext(headers);

				Assert.assertNotNull(routerContext);
				Map<String, String> routerLabels = routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS);
				Assert.assertNotNull(routerLabels);
				Assert.assertEquals("v1", routerLabels.get("k1"));
				Assert.assertEquals("v2", routerLabels.get("k2"));
				Assert.assertNull(routerLabels.get("k3"));
			}
		}
	}

	@Test
	public void testHasNoneRouterContext() {
		DefaultClientConfigImpl config = new DefaultClientConfigImpl();
		config.loadDefaultValues();
		ILoadBalancer loadBalancer = new SimpleLoadBalancer();
		ServerIntrospector serverIntrospector = new DefaultServerIntrospector();

		PolarisFeignLoadBalancer polarisFeignLoadBalancer = new PolarisFeignLoadBalancer(loadBalancer, config, serverIntrospector);

		Map<String, Collection<String>> headers = new HashMap<>();

		// mock ApplicationContextAwareUtils#getProperties
		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn("unit-test");
			MetadataContext metadataContext = Mockito.mock(MetadataContext.class);
			// mock MetadataContextHolder#get
			try (MockedStatic<MetadataContextHolder> mockedMetadataContextHolder = Mockito.mockStatic(MetadataContextHolder.class)) {
				mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);

				PolarisRouterContext routerContext = polarisFeignLoadBalancer.buildRouterContext(headers);

				Assert.assertNull(routerContext);
			}
		}
	}
}
