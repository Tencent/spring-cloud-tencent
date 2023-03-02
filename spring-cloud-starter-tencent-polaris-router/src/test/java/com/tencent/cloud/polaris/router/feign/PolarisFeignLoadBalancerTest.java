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
import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;

/**
 * test for {@link PolarisFeignLoadBalancer}.
 *
 * @author lepdou 2022-05-26
 */
@ExtendWith(MockitoExtension.class)
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
		headers.put(RouterConstant.ROUTER_LABEL_HEADER, headerValues);

		// mock ApplicationContextAwareUtils#getProperties
		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn("unit-test");

			MetadataContext metadataContext = Mockito.mock(MetadataContext.class);
			// mock MetadataContextHolder#get
			try (MockedStatic<MetadataContextHolder> mockedMetadataContextHolder = Mockito.mockStatic(MetadataContextHolder.class)) {
				mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);

				PolarisRouterContext routerContext = polarisFeignLoadBalancer.buildRouterContext(headers);

				assertThat(routerContext).isNotNull();
				Map<String, String> routerLabels = routerContext.getLabels(RouterConstant.ROUTER_LABELS);
				assertThat(routerLabels).isNotNull();
				assertThat(routerLabels.get("k1")).isEqualTo("v1");
				assertThat(routerLabels.get("k2")).isEqualTo("v2");
				assertThat(routerLabels.get("k3")).isNull();
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
				assertThat(routerContext).isNull();
			}
		}
	}
}
