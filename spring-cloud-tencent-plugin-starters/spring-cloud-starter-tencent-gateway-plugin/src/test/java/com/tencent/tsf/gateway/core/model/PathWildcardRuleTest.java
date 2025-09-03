/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.tsf.gateway.core.model;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PathWildcardRule}.
 *
 * @author Haotian Zhang
 */
public class PathWildcardRuleTest {

	@Test
	public void testPathWildcardRule() {
		PathWildcardRule pathWildcardRule = new PathWildcardRule();
		pathWildcardRule.setWildCardId("wildCardId");
		pathWildcardRule.setGroupId("groupId");
		pathWildcardRule.setWildCardPath("wildCardPath");
		pathWildcardRule.setMethod("method");
		pathWildcardRule.setServiceId("serviceId");
		pathWildcardRule.setServiceName("serviceName");
		pathWildcardRule.setNamespaceId("namespaceId");
		pathWildcardRule.setNamespaceName("namespaceName");
		pathWildcardRule.setTimeout(100);
		pathWildcardRule.setWildCardIds(Collections.singletonList("wildCardId"));

		assertThat(pathWildcardRule.getWildCardId()).isEqualTo("wildCardId");
		assertThat(pathWildcardRule.getGroupId()).isEqualTo("groupId");
		assertThat(pathWildcardRule.getWildCardPath()).isEqualTo("wildCardPath");
		assertThat(pathWildcardRule.getMethod()).isEqualTo("method");
		assertThat(pathWildcardRule.getServiceId()).isEqualTo("serviceId");
		assertThat(pathWildcardRule.getServiceName()).isEqualTo("serviceName");
		assertThat(pathWildcardRule.getNamespaceId()).isEqualTo("namespaceId");
		assertThat(pathWildcardRule.getNamespaceName()).isEqualTo("namespaceName");
		assertThat(pathWildcardRule.getTimeout()).isEqualTo(100);
		assertThat(pathWildcardRule.getWildCardIds()).containsExactly("wildCardId");
		assertThat(pathWildcardRule.toString()).isEqualTo("PathWildcardRule{wildCardId='wildCardId', groupId='groupId', wildCardPath='wildCardPath', method='method', serviceId='serviceId', serviceName='serviceName', namespaceId='namespaceId', namespaceName='namespaceName', wildCardIds=[wildCardId], timeout=100}");
	}
}
