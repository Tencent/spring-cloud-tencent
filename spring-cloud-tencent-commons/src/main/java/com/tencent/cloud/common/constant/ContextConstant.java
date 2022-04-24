/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.common.constant;

/**
 * Constant for Context.
 *
 * @author Haotian Zhang
 */
public final class ContextConstant {

	/**
	 * Order of configuration modifier.
	 */
	public static final class ModifierOrder {

		/**
		 * First modifier order.
		 */
		public static Integer FIRST = Integer.MIN_VALUE;

		/**
		 * Last modifier order.
		 */
		public static Integer LAST = Integer.MAX_VALUE;

		/**
		 * Order of circuit breaker configuration modifier.
		 */
		public static Integer CIRCUIT_BREAKER_ORDER = 1;

		/**
		 * Order of discovery configuration modifier.
		 */
		public static Integer DISCOVERY_ORDER = 0;

		/**
		 * Order of configuration modifier.
		 */
		public static Integer CONFIG_ORDER = 1;

	}

}
