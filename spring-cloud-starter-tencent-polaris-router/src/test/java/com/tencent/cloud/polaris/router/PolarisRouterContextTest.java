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
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.tencent.cloud.common.constant.RouterConstant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * test for {@link PolarisRouterContext}.
 *
 *@author lepdou 2022-05-26
 */
public class PolarisRouterContextTest {

	@Test
	public void testNormalGetterSetter() {
		Map<String, String> labels = new HashMap<>();
		labels.put("k1", "v1");
		labels.put("k2", "v2");

		PolarisRouterContext routerContext = new PolarisRouterContext();
		routerContext.putLabels(RouterConstant.ROUTER_LABELS, labels);

		assertThat(routerContext.getLabels(RouterConstant.TRANSITIVE_LABELS).size()).isEqualTo(0);
		assertThat(routerContext.getLabels(RouterConstant.ROUTER_LABELS).size()).isEqualTo(2);
		assertThat(routerContext.getLabels(RouterConstant.ROUTER_LABELS).get("k1")).isEqualTo("v1");
		assertThat(routerContext.getLabels(RouterConstant.ROUTER_LABELS).get("k2")).isEqualTo("v2");
		assertThat(routerContext.getLabels(RouterConstant.ROUTER_LABELS).get("k3")).isNull();
	}

	@Test
	public void testSetNull() {
		PolarisRouterContext routerContext = new PolarisRouterContext();
		routerContext.putLabels(RouterConstant.ROUTER_LABELS, null);
		assertThat(routerContext.getLabels(RouterConstant.TRANSITIVE_LABELS).size()).isEqualTo(0);
		assertThat(routerContext.getLabels(RouterConstant.ROUTER_LABELS).size()).isEqualTo(0);
	}

	@Test
	public void testGetEmptyRouterContext() {
		PolarisRouterContext routerContext = new PolarisRouterContext();
		assertThat(routerContext.getLabels(RouterConstant.TRANSITIVE_LABELS).size()).isEqualTo(0);
		assertThat(routerContext.getLabels(RouterConstant.ROUTER_LABELS).size()).isEqualTo(0);
	}

	@Test
	public void testGetLabelByKeys() {
		Map<String, String> labels = new HashMap<>();
		labels.put("k1", "v1");
		labels.put("k2", "v2");
		labels.put("k3", "v3");

		PolarisRouterContext routerContext = new PolarisRouterContext();
		routerContext.putLabels(RouterConstant.ROUTER_LABELS, labels);

		Map<String, String> resolvedLabels = routerContext.getLabels(RouterConstant.ROUTER_LABELS,
				Sets.newHashSet("k1", "k2", "k4"));

		assertThat(resolvedLabels.size()).isEqualTo(2);
		assertThat(resolvedLabels.get("k1")).isEqualTo("v1");
		assertThat(resolvedLabels.get("k2")).isEqualTo("v2");
	}

	@Test
	public void testGetLabel() {
		Map<String, String> labels = new HashMap<>();
		labels.put("k1", "v1");
		labels.put("k2", "v2");
		labels.put("k3", "v3");

		PolarisRouterContext routerContext = new PolarisRouterContext();
		routerContext.putLabels(RouterConstant.ROUTER_LABELS, labels);

		String resolvedLabel = routerContext.getLabel("k1");
		assertThat(resolvedLabel).isEqualTo("v1");
	}

	@Test
	public void testGetLabelAsSet() {
		Map<String, String> labels = new HashMap<>();
		labels.put("k1", "v1,v2,v3");

		PolarisRouterContext routerContext = new PolarisRouterContext();
		routerContext.putLabels(RouterConstant.ROUTER_LABELS, labels);

		Set<String> resolvedLabels = routerContext.getLabelAsSet("k1");

		assertThat(resolvedLabels.size()).isEqualTo(3);
		assertThat(resolvedLabels).contains("v1");
		assertThat(resolvedLabels).contains("v2");
		assertThat(resolvedLabels).contains("v3");
	}
}
