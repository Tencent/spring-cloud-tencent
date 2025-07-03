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

public class TransformerAction {

	/**
	 * 目前仅支持新增 add，未来可能增加 edit、delete.
	 */
	private String action;

	private Position tagPosition;

	/**
	 * 转化后标签名称.
	 */
	private String tagName;
	/**
	 * 未来可能支持 tagValue 来着动态的.
	 */
	private String tagValue;

	private Integer weight;

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Position getTagPosition() {
		return tagPosition;
	}

	public void setTagPosition(Position tagPosition) {
		this.tagPosition = tagPosition;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public String getTagValue() {
		return tagValue;
	}

	public void setTagValue(String tagValue) {
		this.tagValue = tagValue;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return "TransformerAction{" +
				"action='" + action + '\'' +
				", tagPosition=" + tagPosition +
				", tagName='" + tagName + '\'' +
				", tagValue='" + tagValue + '\'' +
				", weight=" + weight +
				'}';
	}
}
