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
 * Test for {@link GroupResult}.
 *
 * @author Haotian Zhang
 */
public class GroupResultTest {

	@Test
	public void testGroupResult() {
		GroupResult groupResult = new GroupResult();
		groupResult.setGatewayId("gatewayId");
		groupResult.setGatewayName("gatewayName");
		groupResult.setGatewayGroupId("gatewayGroupId");
		groupResult.setReversion(1);
		groupResult.setUpdatedTime("updatedTime");
		groupResult.setResult(new ArrayList<>());
		groupResult.getResult().add(new Group());

		assertThat(groupResult.getGatewayId()).isEqualTo("gatewayId");
		assertThat(groupResult.getGatewayName()).isEqualTo("gatewayName");
		assertThat(groupResult.getGatewayGroupId()).isEqualTo("gatewayGroupId");
		assertThat(groupResult.getReversion()).isEqualTo(1);
		assertThat(groupResult.getUpdatedTime()).isEqualTo("updatedTime");
		assertThat(groupResult.getResult()).isNotEmpty();
		assertThat(groupResult.getResult()).hasSize(1);
	}
}
