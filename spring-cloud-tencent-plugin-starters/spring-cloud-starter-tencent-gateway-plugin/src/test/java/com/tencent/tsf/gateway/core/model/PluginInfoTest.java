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

import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PluginInfo}.
 *
 * @author Haotian Zhang
 */
public class PluginInfoTest {

	@Test
	public void testPluginInfo() {
		PluginInfo pluginInfo = new PluginInfo();

		assertThatThrownBy(pluginInfo::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("插件名称参数错误");
		pluginInfo.setName("name");

		assertThatThrownBy(pluginInfo::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("插件类型参数错误");
		pluginInfo.setType("type");

		pluginInfo.setId("id");
		pluginInfo.setOrder(1);
		pluginInfo.setDescription("description");
		pluginInfo.setCreatedTime("createdTime");
		pluginInfo.setUpdatedTime("updatedTime");
		assertThatNoException().isThrownBy(pluginInfo::check);

		assertThat(pluginInfo.getId()).isEqualTo("id");
		assertThat(pluginInfo.getName()).isEqualTo("name");
		assertThat(pluginInfo.getType()).isEqualTo("type");
		assertThat(pluginInfo.getOrder()).isEqualTo(1);
		assertThat(pluginInfo.getDescription()).isEqualTo("description");
		assertThat(pluginInfo.getCreatedTime()).isEqualTo("createdTime");
		assertThat(pluginInfo.getUpdatedTime()).isEqualTo("updatedTime");
		assertThat(pluginInfo.toString()).isEqualTo("PluginInfo{id='id', name='name', type='type', order=1, description='description', createdTime='createdTime', updatedTime='updatedTime'}");
	}
}
