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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link GroupApiResult}.
 *
 * @author Haotian Zhang
 */
public class GroupApiResultTest {

	@Test
	public void testGroupApiResult() {
		GroupApiResult groupApiResult = new GroupApiResult();
		groupApiResult.setGatewayId("gatewayId");
		groupApiResult.setGatewayName("gatewayName");
		groupApiResult.setGatewayGroupId("gatewayGroupId");
		groupApiResult.setReversion(1);
		groupApiResult.setUpdatedTime("updatedTime");
		groupApiResult.setResult(new ArrayList<>());
		groupApiResult.getResult().add(new GroupApi());

		assertThat(groupApiResult.getGatewayId()).isEqualTo("gatewayId");
		assertThat(groupApiResult.getGatewayName()).isEqualTo("gatewayName");
		assertThat(groupApiResult.getGatewayGroupId()).isEqualTo("gatewayGroupId");
		assertThat(groupApiResult.getReversion()).isEqualTo(1);
		assertThat(groupApiResult.getUpdatedTime()).isEqualTo("updatedTime");
		assertThat(groupApiResult.getResult()).isNotEmpty();
		assertThat(groupApiResult.getResult()).hasSize(1);
	}
}
