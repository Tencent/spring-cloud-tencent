/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
		if (!(o instanceof UnitTag tag)) {
			return false;
		}
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
