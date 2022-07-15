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
import org.junit.Assert;
import org.junit.Test;

/**
 * test for {@link PolarisRouterContext}.
 *
 * @author lepdou 2022-05-26
 */
public class PolarisRouterContextTest {

	@Test
	public void testNormalGetterSetter() {
		Map<String, String> labels = new HashMap<>();
		labels.put("k1", "v1");
		labels.put("k2", "v2");

		PolarisRouterContext routerContext = new PolarisRouterContext();
		routerContext.putLabels(PolarisRouterContext.ROUTER_LABELS, labels);

		Assert.assertEquals(0, routerContext.getLabels(PolarisRouterContext.TRANSITIVE_LABELS).size());
		Assert.assertEquals(2, routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS).size());
		Assert.assertEquals("v1", routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS).get("k1"));
		Assert.assertEquals("v2", routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS).get("k2"));
		Assert.assertNull(routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS).get("k3"));
	}

	@Test
	public void testSetNull() {
		PolarisRouterContext routerContext = new PolarisRouterContext();
		routerContext.putLabels(PolarisRouterContext.ROUTER_LABELS, null);
		Assert.assertEquals(0, routerContext.getLabels(PolarisRouterContext.TRANSITIVE_LABELS).size());
		Assert.assertEquals(0, routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS).size());
	}

	@Test
	public void testGetEmptyRouterContext() {
		PolarisRouterContext routerContext = new PolarisRouterContext();
		Assert.assertEquals(0, routerContext.getLabels(PolarisRouterContext.TRANSITIVE_LABELS).size());
		Assert.assertEquals(0, routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS).size());
	}

	@Test
	public void testGetLabelByKeys() {
		Map<String, String> labels = new HashMap<>();
		labels.put("k1", "v1");
		labels.put("k2", "v2");
		labels.put("k3", "v3");

		PolarisRouterContext routerContext = new PolarisRouterContext();
		routerContext.putLabels(PolarisRouterContext.ROUTER_LABELS, labels);

		Map<String, String> resolvedLabels = routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS,
				Sets.newHashSet("k1", "k2", "k4"));

		Assert.assertEquals(2, resolvedLabels.size());
		Assert.assertEquals("v1", resolvedLabels.get("k1"));
		Assert.assertEquals("v2", resolvedLabels.get("k2"));
	}

	@Test
	public void testGetLabel() {
		Map<String, String> labels = new HashMap<>();
		labels.put("k1", "v1");
		labels.put("k2", "v2");
		labels.put("k3", "v3");

		PolarisRouterContext routerContext = new PolarisRouterContext();
		routerContext.putLabels(PolarisRouterContext.ROUTER_LABELS, labels);

		String resolvedLabel = routerContext.getLabel("k1");

		Assert.assertEquals("v1", resolvedLabel);
	}

	@Test
	public void testGetLabelAsSet() {
		Map<String, String> labels = new HashMap<>();
		labels.put("k1", "v1,v2,v3");

		PolarisRouterContext routerContext = new PolarisRouterContext();
		routerContext.putLabels(PolarisRouterContext.ROUTER_LABELS, labels);

		Set<String> resolvedLabels = routerContext.getLabelAsSet("k1");

		Assert.assertEquals(3, resolvedLabels.size());
		Assert.assertTrue(resolvedLabels.contains("v1"));
		Assert.assertTrue(resolvedLabels.contains("v2"));
		Assert.assertTrue(resolvedLabels.contains("v3"));
	}
}
