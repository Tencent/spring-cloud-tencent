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
 * Built-in system http header fields.
 */
public final class HeaderConstant {

	/**
	 * The called service returns the real call result of its own processing request.
	 */
	public static final String INTERNAL_CALLEE_RET_STATUS = "internal-callee-retstatus";

	/**
	 * The name of the rule that the current limit/circiutbreaker rule takes effect.
	 */
	public static final String INTERNAL_ACTIVE_RULE_NAME = "internal-callee-activerule";

	/**
	 * The name information of the called service.
	 */
	public static final String INTERNAL_CALLEE_SERVICE_ID = "internal-callee-serviceid";

	/**
	 * The name information of the called instance host.
	 */
	public static final String INTERNAL_CALLEE_INSTANCE_HOST = "internal-callee-instance-host";

	/**
	 * The name information of the called instance port.
	 */
	public static final String INTERNAL_CALLEE_INSTANCE_PORT = "internal-callee-instance-port";

	private HeaderConstant() {
	}
}
