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

import com.tencent.cloud.plugin.gateway.context.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link TransformerAction}.
 *
 * @author Haotian Zhang
 */
public class TransformerActionTest {

	@Test
	public void testTransformerAction() {
		TransformerAction transformerAction = new TransformerAction();
		transformerAction.setAction("add");
		transformerAction.setTagPosition(Position.HEADER);
		transformerAction.setTagName("tagName");
		transformerAction.setTagValue("tagValue");
		transformerAction.setWeight(1);

		assertThat(transformerAction.getAction()).isEqualTo("add");
		assertThat(transformerAction.getTagPosition()).isEqualTo(Position.HEADER);
		assertThat(transformerAction.getTagName()).isEqualTo("tagName");
		assertThat(transformerAction.getTagValue()).isEqualTo("tagValue");
		assertThat(transformerAction.getWeight()).isEqualTo(1);
		assertThat(transformerAction.toString()).isEqualTo("TransformerAction{action='add', tagPosition=HEADER, tagName='tagName', tagValue='tagValue', weight=1}");
	}
}
