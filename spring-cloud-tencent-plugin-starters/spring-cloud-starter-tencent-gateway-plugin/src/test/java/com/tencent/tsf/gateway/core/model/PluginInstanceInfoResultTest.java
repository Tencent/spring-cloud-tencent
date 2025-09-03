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
 * Test for {@link PluginInstanceInfoResult}.
 *
 * @author Haotian Zhang
 */
public class PluginInstanceInfoResultTest {

	@Test
	public void testPluginInstanceInfoResult() {
		PluginInstanceInfoResult pluginInstanceInfoResult = new PluginInstanceInfoResult();
		pluginInstanceInfoResult.setGatewayId("gatewayId");
		pluginInstanceInfoResult.setGatewayName("gatewayName");
		pluginInstanceInfoResult.setGatewayGroupId("gatewayGroupId");
		pluginInstanceInfoResult.setReversion(1);
		pluginInstanceInfoResult.setUpdatedTime("updatedTime");
		pluginInstanceInfoResult.setResult(new ArrayList<>());
		pluginInstanceInfoResult.getResult().add(new PluginInstanceInfo());

		assertThat(pluginInstanceInfoResult.getGatewayId()).isEqualTo("gatewayId");
		assertThat(pluginInstanceInfoResult.getGatewayName()).isEqualTo("gatewayName");
		assertThat(pluginInstanceInfoResult.getGatewayGroupId()).isEqualTo("gatewayGroupId");
		assertThat(pluginInstanceInfoResult.getReversion()).isEqualTo(1);
		assertThat(pluginInstanceInfoResult.getUpdatedTime()).isEqualTo("updatedTime");
		assertThat(pluginInstanceInfoResult.getResult()).isNotEmpty();
		assertThat(pluginInstanceInfoResult.getResult()).hasSize(1);
	}
}
