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
 * Test for {@link GroupSecret}.
 *
 * @author Haotian Zhang
 */
public class GroupSecretTest {

	@Test
	public void testGroupSecret() {
		GroupSecret groupSecret = new GroupSecret();
		groupSecret.setSecretId("secretId");
		groupSecret.setSecretKey("secretKey");
		groupSecret.setSecretName("secretName");
		groupSecret.setGroupId("groupId");
		groupSecret.setStatus("status");
		groupSecret.setExpiredTime("expiredTime");

		assertThat(groupSecret.getSecretId()).isEqualTo("secretId");
		assertThat(groupSecret.getSecretKey()).isEqualTo("secretKey");
		assertThat(groupSecret.getSecretName()).isEqualTo("secretName");
		assertThat(groupSecret.getGroupId()).isEqualTo("groupId");
		assertThat(groupSecret.getStatus()).isEqualTo("status");
		assertThat(groupSecret.getExpiredTime()).isEqualTo("expiredTime");
		assertThat(groupSecret.toString()).isEqualTo("GroupSecret{secretId='secretId', secretKey='secretKey', secretName='secretName', groupId='groupId', status='status', expiredTime='expiredTime'}");
	}
}
