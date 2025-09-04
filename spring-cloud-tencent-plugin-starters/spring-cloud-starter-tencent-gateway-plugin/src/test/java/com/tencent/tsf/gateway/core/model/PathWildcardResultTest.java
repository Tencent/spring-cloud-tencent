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
 * Test for {@link PathWildcardResult}.
 *
 * @author Haotian Zhang
 */
public class PathWildcardResultTest {

	@Test
	public void testPathWildcardResult() {
		PathWildcardResult pathWildcardResult = new PathWildcardResult();
		pathWildcardResult.setGatewayId("gatewayId");
		pathWildcardResult.setGatewayName("gatewayName");
		pathWildcardResult.setGatewayGroupId("gatewayGroupId");
		pathWildcardResult.setReversion(1);
		pathWildcardResult.setUpdatedTime("updatedTime");
		pathWildcardResult.setResult(new ArrayList<>());
		pathWildcardResult.getResult().add(new PathWildcardRule());

		assertThat(pathWildcardResult.getGatewayId()).isEqualTo("gatewayId");
		assertThat(pathWildcardResult.getGatewayName()).isEqualTo("gatewayName");
		assertThat(pathWildcardResult.getGatewayGroupId()).isEqualTo("gatewayGroupId");
		assertThat(pathWildcardResult.getReversion()).isEqualTo(1);
		assertThat(pathWildcardResult.getUpdatedTime()).isEqualTo("updatedTime");
		assertThat(pathWildcardResult.getResult()).isNotEmpty();
		assertThat(pathWildcardResult.getResult()).hasSize(1);
	}
}
