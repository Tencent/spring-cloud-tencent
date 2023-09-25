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
 *
 */

package com.tencent.cloud.rpc.enhancement.plugin;

import org.springframework.core.Ordered;

/**
 * PluginOrderConstant.
 *
 * @author sean yu
 */
public class PluginOrderConstant {

	public static class ClientPluginOrder {

		/**
		 * order for
		 * {@link com.tencent.cloud.rpc.enhancement.plugin.reporter.SuccessPolarisReporter}
		 * and
		 * {@link com.tencent.cloud.rpc.enhancement.plugin.reporter.ExceptionPolarisReporter}.
		 */
		public static final int CONSUMER_REPORTER_PLUGIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 1;

		/**
		 * order for
		 * {@link com.tencent.cloud.polaris.circuitbreaker.reporter.SuccessCircuitBreakerReporter}
		 * and
		 * {@link com.tencent.cloud.polaris.circuitbreaker.reporter.ExceptionCircuitBreakerReporter}.
		 */
		public static final int CIRCUIT_BREAKER_REPORTER_PLUGIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 2;
	}
}
