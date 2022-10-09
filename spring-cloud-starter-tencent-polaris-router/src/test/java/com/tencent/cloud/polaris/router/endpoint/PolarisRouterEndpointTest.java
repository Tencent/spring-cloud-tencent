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

package com.tencent.cloud.polaris.router.endpoint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.polaris.client.pb.ModelProto;
import com.tencent.polaris.client.pb.RoutingProto;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisRouterEndpoint}.
 * @author lepdou 2022-07-25
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisRouterEndpointTest {

	private static final String testDestService = "dstService";
	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;

	@Mock
	private ServiceRuleManager serviceRuleManager;
	@InjectMocks
	private PolarisRouterEndpoint polarisRouterEndpoint;

	@BeforeClass
	public static void beforeClass() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn(testDestService);
	}

	@AfterClass
	public static void afterClass() {
		mockedApplicationContextAwareUtils.close();
	}

	@Test
	public void testHasRouterRule() {
		Map<String, ModelProto.MatchString> labels = new HashMap<>();
		ModelProto.MatchString matchString = ModelProto.MatchString.getDefaultInstance();
		String validKey1 = "${http.header.uid}";
		String validKey2 = "${http.query.name}";
		String validKey3 = "${http.method}";
		String validKey4 = "${http.uri}";
		String validKey5 = "${http.body.customkey}";
		String invalidKey = "$http.expression.wrong}";
		labels.put(validKey1, matchString);
		labels.put(validKey2, matchString);
		labels.put(validKey3, matchString);
		labels.put(validKey4, matchString);
		labels.put(validKey5, matchString);
		labels.put(invalidKey, matchString);

		RoutingProto.Source source1 = RoutingProto.Source.newBuilder().putAllMetadata(labels).build();
		RoutingProto.Source source2 = RoutingProto.Source.newBuilder().putAllMetadata(labels).build();
		RoutingProto.Source source3 = RoutingProto.Source.newBuilder().putAllMetadata(new HashMap<>()).build();

		List<RoutingProto.Route> routes = new LinkedList<>();
		RoutingProto.Route route = RoutingProto.Route.newBuilder()
				.addAllSources(Lists.newArrayList(source1, source2, source3))
				.build();
		routes.add(route);

		when(serviceRuleManager.getServiceRouterRule(testDestService, testDestService, testDestService)).thenReturn(routes);

		Map<String, Object> actuator = polarisRouterEndpoint.router(testDestService);

		Assert.assertNotNull(actuator.get("routerRules"));
		Assert.assertEquals(1, ((List) actuator.get("routerRules")).size());
	}

	@Test
	public void testHasNotRouterRule() {
		List<RoutingProto.Route> routes = new LinkedList<>();

		when(serviceRuleManager.getServiceRouterRule(testDestService, testDestService, testDestService)).thenReturn(routes);

		Map<String, Object> actuator = polarisRouterEndpoint.router(testDestService);

		Assert.assertNotNull(actuator.get("routerRules"));
		Assert.assertEquals(0, ((List) actuator.get("routerRules")).size());
	}
}
