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

import com.tencent.cloud.plugin.gateway.context.PathRewrite;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PathRewriteResult}.
 *
 * @author Haotian Zhang
 */
public class PathRewriteResultTest {

	@Test
	public void testPathRewriteResult() {
		PathRewriteResult pathRewriteResult = new PathRewriteResult();
		pathRewriteResult.setGatewayId("gatewayId");
		pathRewriteResult.setGatewayName("gatewayName");
		pathRewriteResult.setGatewayGroupId("gatewayGroupId");
		pathRewriteResult.setReversion(1);
		pathRewriteResult.setUpdatedTime("updatedTime");
		pathRewriteResult.setResult(new ArrayList<>());
		pathRewriteResult.getResult().add(new PathRewrite());

		assertThat(pathRewriteResult.getGatewayId()).isEqualTo("gatewayId");
		assertThat(pathRewriteResult.getGatewayName()).isEqualTo("gatewayName");
		assertThat(pathRewriteResult.getGatewayGroupId()).isEqualTo("gatewayGroupId");
		assertThat(pathRewriteResult.getReversion()).isEqualTo(1);
		assertThat(pathRewriteResult.getUpdatedTime()).isEqualTo("updatedTime");
		assertThat(pathRewriteResult.getResult()).isNotEmpty();
		assertThat(pathRewriteResult.getResult()).hasSize(1);
	}
}
