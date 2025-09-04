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

import com.tencent.cloud.plugin.gateway.context.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link RequestTransformerPluginInfo}.
 *
 * @author Haotian Zhang
 */
public class RequestTransformerPluginInfoTest {

	@Test
	public void testRequestTransformerPluginInfo() {
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
		transformerAction.setWeight(100);
		actions.add(transformerAction);
		requestTransformerPluginInfo.setActions(actions);

		assertThat(requestTransformerPluginInfo.toString()).isEqualTo("RequestTransformerPluginInfo{filters=[TransformerTag{tagPosition=COOKIE} TagCondition{tagId=null, tagType='null', tagField='null', tagOperator='null', tagValue='null'}], actions=[TransformerAction{action='add', tagPosition=HEADER, tagName='tagName', tagValue='tagValue', weight=100}]}");
	}
}
