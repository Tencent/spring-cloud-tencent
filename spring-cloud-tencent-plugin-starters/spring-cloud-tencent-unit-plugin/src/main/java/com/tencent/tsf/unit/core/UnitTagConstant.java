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
