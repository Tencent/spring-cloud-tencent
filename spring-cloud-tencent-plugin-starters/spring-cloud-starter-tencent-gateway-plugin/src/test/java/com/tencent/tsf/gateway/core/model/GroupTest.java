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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Group}.
 *
 * @author Haotian Zhang
 */
public class GroupTest {

	@Test
	public void testGroup() {
		Group group = new Group();
		group.setGroupId("groupId");
		group.setGroupName("groupName");
		group.setGroupContext("groupContext");
		group.setReleaseStatus("releaseStatus");
		group.setAuthMode("authMode");
		group.setGroupType("groupType");
		group.setSecretList(generateGroupSecretList());
		group.setNamespaceNameKey("namespaceNameKey");
		group.setServiceNameKey("serviceNameKey");
		group.setNamespaceNameKeyPosition("Header");
		group.setServiceNameKeyPosition("Query");

		assertThat(group.getGroupId()).isEqualTo("groupId");
		assertThat(group.getGroupName()).isEqualTo("groupName");
		assertThat(group.getGroupContext()).isEqualTo("groupContext");
		assertThat(group.getReleaseStatus()).isEqualTo("releaseStatus");
		assertThat(group.getAuthMode()).isEqualTo("authMode");
		assertThat(group.getGroupType()).isEqualTo("groupType");
		assertThat(group.getSecretList()).isNotNull();
		assertThat(group.getSecretList()).hasSize(1);
		assertThat(group.getNamespaceNameKey()).isEqualTo("namespaceNameKey");
		assertThat(group.getServiceNameKey()).isEqualTo("serviceNameKey");
		assertThat(group.getNamespaceNameKeyPosition()).isEqualTo("Header");
		assertThat(group.getServiceNameKeyPosition()).isEqualTo("Query");
		assertThat(group.toString()).isEqualTo("Group{groupId='groupId', groupName='groupName', groupContext='groupContext', releaseStatus='releaseStatus', authMode='authMode', groupType='groupType', secretList=[GroupSecret{secretId='secretId', secretKey='secretKey', secretName='secretName', groupId='groupId', status='status', expiredTime='expiredTime'}], namespaceNameKey='namespaceNameKey', serviceNameKey='serviceNameKey', namespaceNameKeyPosition='Header', serviceNameKeyPosition='Query'}");
	}

	private List<GroupSecret> generateGroupSecretList() {
		GroupSecret groupSecret = new GroupSecret();
		groupSecret.setSecretId("secretId");
		groupSecret.setSecretKey("secretKey");
		groupSecret.setSecretName("secretName");
		groupSecret.setGroupId("groupId");
		groupSecret.setStatus("status");
		groupSecret.setExpiredTime("expiredTime");
		List<GroupSecret> groupSecretList = new ArrayList<>();
		groupSecretList.add(groupSecret);
		return groupSecretList;
	}
}
