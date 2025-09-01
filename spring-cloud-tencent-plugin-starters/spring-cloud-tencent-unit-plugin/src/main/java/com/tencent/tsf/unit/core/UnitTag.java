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

package com.tencent.tsf.unit.core;


import java.util.Objects;

import com.tencent.tsf.unit.core.model.UnitTagPosition;

public class UnitTag {

	private UnitTagPosition tagPosition;
	/**
	 * 标签名.
	 */
	private String tagField;

	/**
	 * 标签运算符.
	 * 定义在 UnitTagConstant.OPERATOR
	 */
	private String tagOperator;

	/**
	 * 标签的被运算对象值.
	 */
	private String tagValue;

	public UnitTag() {
	}

	public UnitTag(String tagField, String tagValue) {
		this.tagField = tagField;
		this.tagValue = tagValue;
	}

	@Override
	public String toString() {
		return "UnitTag{" +
				"position=" + tagPosition +
				", tagField='" + tagField + '\'' +
				", tagOperator='" + tagOperator + '\'' +
				", tagValue='" + tagValue + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UnitTag)) {
			return false;
		}
		UnitTag tag = (UnitTag) o;
		return Objects.equals(tagPosition, tag.tagPosition) &&
				tagField.equals(tag.tagField) &&
				tagOperator.equals(tag.tagOperator) &&
				tagValue.equals(tag.tagValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tagField, tagOperator, tagValue);
	}

	public UnitTagPosition getTagPosition() {
		return tagPosition;
	}

	public void setTagPosition(UnitTagPosition tagPosition) {
		this.tagPosition = tagPosition;
	}

	public String getTagField() {
		return tagField;
	}

	public void setTagField(String tagField) {
		this.tagField = tagField;
	}

	public String getTagOperator() {
		return tagOperator;
	}

	public void setTagOperator(String tagOperator) {
		this.tagOperator = tagOperator;
	}

	public String getTagValue() {
		return tagValue;
	}

	public void setTagValue(String tagValue) {
		this.tagValue = tagValue;
	}
}
