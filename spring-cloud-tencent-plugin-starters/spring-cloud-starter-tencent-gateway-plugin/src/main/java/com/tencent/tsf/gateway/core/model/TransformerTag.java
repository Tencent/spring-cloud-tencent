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
import com.tencent.polaris.plugins.connector.consul.service.common.TagCondition;

public class TransformerTag extends TagCondition {

	private Position tagPosition;

	public Position getTagPosition() {
		return tagPosition;
	}

	public void setTagPosition(Position tagPosition) {
		this.tagPosition = tagPosition;
	}

	@Override
	public String toString() {
		return "TransformerTag{" +
				"tagPosition=" + tagPosition +
				"} " + super.toString();
	}
}
