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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * test for {@link RouterRuleLabelResolver}.
 *@author lepdou 2022-05-26
 */
@ExtendWith(MockitoExtension.class)
public class RouterRuleLabelResolverTest {

	private final String testNamespace = "testNamespace";
	private final String testSourceService = "sourceService";
	private final String testDstService = "dstService";
	@Mock
	private ServiceRuleManager serviceRuleManager;

	@Test
	public void test() {
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
				.addAllSources(Lists.list(source1, source2, source3))
				.build();
		routes.add(route);

		when(serviceRuleManager.getServiceRouterRule(testNamespace, testSourceService, testDstService)).thenReturn(routes);

		RouterRuleLabelResolver resolver = new RouterRuleLabelResolver(serviceRuleManager);

		Set<String> resolvedExpressionLabelKeys = resolver.getExpressionLabelKeys(testNamespace, testSourceService, testDstService);

		assertThat(resolvedExpressionLabelKeys).isNotNull();
		assertThat(resolvedExpressionLabelKeys.size()).isEqualTo(6);
		assertThat(resolvedExpressionLabelKeys).contains(validKey1);
		assertThat(resolvedExpressionLabelKeys).contains(validKey2);
		assertThat(resolvedExpressionLabelKeys).contains(validKey3);
		assertThat(resolvedExpressionLabelKeys).contains(validKey4);
		assertThat(resolvedExpressionLabelKeys).contains(validKey5);
		assertThat(resolvedExpressionLabelKeys).contains(invalidKey);
	}
}
