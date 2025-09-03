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
 * Test for {@link TagPlugin}.
 *
 * @author Haotian Zhang
 */
public class TagPluginTest {

	@Test
	public void testTagPlugin() {
		TagPlugin tagPlugin = new TagPlugin();

		assertThatThrownBy(tagPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("插件名称参数错误");
		tagPlugin.setName("tag");

		assertThatThrownBy(tagPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("插件类型参数错误");
		tagPlugin.setType("Tag");

		assertThatThrownBy(tagPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("验证Tag插件参数");
		tagPlugin.setTagPluginInfoList("[{\"tagPosition\":\"QUERY\",\"preTagName\":\"preTagName\",\"postTagName\":\"postTagName\",\"traceIdEnabled\":\"Y\"");

		assertThatThrownBy(tagPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("验证Tag插件参数");
		tagPlugin.setTagPluginInfoList("[{\"tagPosition\":\"QUERY\",\"preTagName\":\"preTagName\",\"postTagName\":\"postTagName\",\"traceIdEnabled\":\"Y\"}]");

		assertThatNoException().isThrownBy(tagPlugin::check);

		assertThat(tagPlugin.getTagPluginInfoList()).isNotBlank();
	}
}
