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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link GroupApi}.
 *
 * @author Haotian Zhang
 */
public class GroupApiTest {

	@Test
	public void testGroupApi() {
		GroupApi groupApi = new GroupApi();
		groupApi.setApiId("apiId");
		groupApi.setGroupId("groupId");
		groupApi.setPath("path");
		groupApi.setMethod("method");
		groupApi.setServiceName("serviceName");
		groupApi.setNamespaceId("namespaceId");
		groupApi.setNamespaceName("namespaceName");
		groupApi.setReleaseStatus("releaseStatus");
		groupApi.setUsableStatus("usableStatus");
		groupApi.setPathMapping("pathMapping");
		groupApi.setTimeout(100);
		groupApi.setHost("host");
		groupApi.setDescription("description");
		groupApi.setApiType("apiType");
		groupApi.setRpcType("rpcType");
		groupApi.setRpcExt("rpcExt");
		Object rpcExtObj = new Object();
		groupApi.setRpcExtObj(rpcExtObj);

		assertThat(groupApi.getApiId()).isEqualTo("apiId");
		assertThat(groupApi.getGroupId()).isEqualTo("groupId");
		assertThat(groupApi.getPath()).isEqualTo("path");
		assertThat(groupApi.getMethod()).isEqualTo("method");
		assertThat(groupApi.getServiceName()).isEqualTo("serviceName");
		assertThat(groupApi.getNamespaceId()).isEqualTo("namespaceId");
		assertThat(groupApi.getNamespaceName()).isEqualTo("namespaceName");
		assertThat(groupApi.getReleaseStatus()).isEqualTo("releaseStatus");
		assertThat(groupApi.getUsableStatus()).isEqualTo("usableStatus");
		assertThat(groupApi.getPathMapping()).isEqualTo("pathMapping");
		assertThat(groupApi.getTimeout()).isEqualTo(100);
		assertThat(groupApi.getHost()).isEqualTo("host");
		assertThat(groupApi.getDescription()).isEqualTo("description");
		assertThat(groupApi.getApiType()).isEqualTo("apiType");
		assertThat(groupApi.getRpcType()).isEqualTo("rpcType");
		assertThat(groupApi.getRpcExt()).isEqualTo("rpcExt");
		assertThat(groupApi.getRpcExtObj()).isEqualTo(rpcExtObj);
	}
}
