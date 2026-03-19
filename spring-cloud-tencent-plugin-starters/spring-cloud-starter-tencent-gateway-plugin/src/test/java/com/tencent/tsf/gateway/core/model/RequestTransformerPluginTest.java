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
import java.util.List;

import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.plugin.gateway.context.Position;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link RequestTransformerPlugin}.
 *
 * @author Haotian Zhang
 */
public class RequestTransformerPluginTest {

	@Test
	public void testRequestTransformerPlugin() {
		RequestTransformerPlugin requestTransformerPlugin = new RequestTransformerPlugin();

		assertThatThrownBy(requestTransformerPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("插件名称参数错误");
		requestTransformerPlugin.setName("reqTransformer");

		assertThatThrownBy(requestTransformerPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("插件类型参数错误");
		requestTransformerPlugin.setType("ReqTransformer");

		assertThatThrownBy(requestTransformerPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("验证插件参数");
		requestTransformerPlugin.setPluginInfo(generateRequestTransformerPluginInfo(-1).substring(2));

		assertThatThrownBy(requestTransformerPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("验证插件格式");
		requestTransformerPlugin.setPluginInfo(generateRequestTransformerPluginInfo(-1));

		assertThatThrownBy(requestTransformerPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("权重值不合法");
		requestTransformerPlugin.setPluginInfo(generateRequestTransformerPluginInfo(null));
		assertThatThrownBy(requestTransformerPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("权重值不合法");
		requestTransformerPlugin.setPluginInfo(generateRequestTransformerPluginInfo(101));
		assertThatThrownBy(requestTransformerPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("验证插件权重失败，当前权重总和为");
		requestTransformerPlugin.setPluginInfo(generateRequestTransformerPluginInfo(50));

		assertThatNoException().isThrownBy(requestTransformerPlugin::check);
		assertThat(requestTransformerPlugin.toString()).isEqualTo("PluginInfo{id='null', name='reqTransformer', type='ReqTransformer', order=null, description='null', createdTime='null', updatedTime='null'}");
		requestTransformerPlugin.setRequestTransformerPluginInfo(JacksonUtils.deserialize(generateRequestTransformerPluginInfo(50), new TypeReference<RequestTransformerPluginInfo>() { }));
		assertThatNoException().isThrownBy(requestTransformerPlugin::check);

		assertThat(requestTransformerPlugin.getPluginInfo()).isEqualTo(generateRequestTransformerPluginInfo(50));
		assertThat(requestTransformerPlugin.getRequestTransformerPluginInfo()).isNotNull();
		assertThat(requestTransformerPlugin.toString()).isEqualTo("PluginInfo{id='null', name='reqTransformer', type='ReqTransformer', order=null, description='null', createdTime='null', updatedTime='null'}");
	}

	private String generateRequestTransformerPluginInfo(Integer weight) {
		RequestTransformerPluginInfo requestTransformerPluginInfo = new RequestTransformerPluginInfo();
		List<TransformerTag> filters = new ArrayList<>();
		TransformerTag transformerTag = new TransformerTag();
		transformerTag.setTagPosition(Position.COOKIE);
		filters.add(transformerTag);
		requestTransformerPluginInfo.setFilters(filters);
		List<TransformerAction> actions = new ArrayList<>();
		TransformerAction transformerAction = new TransformerAction();
		transformerAction.setAction("add");
		transformerAction.setTagPosition(Position.HEADER);
		transformerAction.setTagName("tagName");
		transformerAction.setTagValue("tagValue");
		if (weight != null) {
			transformerAction.setWeight(weight);
		}
		actions.add(transformerAction);
		requestTransformerPluginInfo.setActions(actions);
		return JacksonUtils.serialize2Json(requestTransformerPluginInfo);
	}
}
