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
 * Test for {@link PluginDetail}.
 *
 * @author Haotian Zhang
 */
public class PluginDetailTest {

	@Test
	public void testPluginDetail() {
		PluginDetail pluginDetail = new PluginDetail();
		pluginDetail.setId("id");
		pluginDetail.setPluginArgInfos(new ArrayList<>());
		PluginArgInfo pluginArgInfo = new PluginArgInfo();
		pluginArgInfo.setId("123");
		pluginArgInfo.setPluginId("456");
		pluginArgInfo.setKey("key");
		pluginArgInfo.setValue("value");
		pluginDetail.getPluginArgInfos().add(pluginArgInfo);

		PluginDetail otherPluginDetail = new PluginDetail();
		otherPluginDetail.setId("id");
		otherPluginDetail.setPluginArgInfos(new ArrayList<>());
		otherPluginDetail.getPluginArgInfos().add(pluginArgInfo);

		assertThat(pluginDetail).isEqualTo(pluginDetail);
		assertThat(pluginDetail).isNotEqualTo(pluginArgInfo);
		assertThat(pluginDetail).isEqualTo(otherPluginDetail);
		assertThat(pluginDetail.hashCode()).isEqualTo(otherPluginDetail.hashCode());
	}
}
