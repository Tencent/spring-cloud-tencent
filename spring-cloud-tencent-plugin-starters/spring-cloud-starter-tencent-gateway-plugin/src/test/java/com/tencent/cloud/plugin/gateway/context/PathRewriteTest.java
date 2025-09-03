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

package com.tencent.cloud.plugin.gateway.context;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PathRewrite}.
 *
 * @author Haotian Zhang
 */
public class PathRewriteTest {

	@Test
	public void basicProperties() {
		PathRewrite one = new PathRewrite();
		one.setPathRewriteId("id");
		one.setGatewayGroupId("gatewayGroupId");
		one.setRegex("regex");
		one.setReplacement("replacement");
		one.setBlocked("Y");
		one.setOrder(1);
		one.setPathRewriteIds(Collections.emptyList());

		assertThat(one.getPathRewriteId()).isEqualTo("id");
		assertThat(one.getGatewayGroupId()).isEqualTo("gatewayGroupId");
		assertThat(one.getRegex()).isEqualTo("regex");
		assertThat(one.getReplacement()).isEqualTo("replacement");
		assertThat(one.getBlocked()).isEqualTo("Y");
		assertThat(one.getOrder()).isEqualTo(1);
		assertThat(one.getPathRewriteIds()).hasSize(0);
		assertThat(one.toString()).isNotEmpty();
	}
}
