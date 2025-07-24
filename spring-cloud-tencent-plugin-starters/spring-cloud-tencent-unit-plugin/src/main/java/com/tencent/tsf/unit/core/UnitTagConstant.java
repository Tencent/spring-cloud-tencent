/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core;

public class UnitTagConstant {

	/**
	 * 规则之间运算表达式的逻辑关系.
	 */
	public static class TagRuleRelation {
		/**
		 * 与.
		 */
		public static final String AND = "AND";
		/**
		 * 或.
		 */
		public static final String OR = "OR";
	}

	/**
	 * 操作符.
	 */
	public static class OPERATOR {

		/**
		 * 包含.
		 */
		public static final String IN = "IN";
		/**
		 * 不包含.
		 */
		public static final String NOT_IN = "NOT_IN";
		/**
		 * 等于.
		 */
		public static final String EQUAL = "EQUAL";
		/**
		 * 不等于.
		 */
		public static final String NOT_EQUAL = "NOT_EQUAL";
		/**
		 * 正则.
		 */
		public static final String REGEX = "REGEX";
		/**
		 * 范围.
		 */
		public static final String RANGE = "RANGE";
	}

}
