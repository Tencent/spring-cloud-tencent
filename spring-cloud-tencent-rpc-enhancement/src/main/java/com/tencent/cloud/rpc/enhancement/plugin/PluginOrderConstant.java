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
		public static final int CONSUMER_REPORTER_PLUGIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

		/**
		 * order for
		 * {@link com.tencent.cloud.polaris.circuitbreaker.reporter.SuccessCircuitBreakerReporter}
		 * and
		 * {@link com.tencent.cloud.polaris.circuitbreaker.reporter.ExceptionCircuitBreakerReporter}.
		 */
		public static final int CIRCUIT_BREAKER_REPORTER_PLUGIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 30;

		/**
		 * order for
		 * {@link com.tencent.cloud.plugin.trafficmirroring.TrafficMirrorPostPlugin}
		 * and
		 * {@link com.tencent.cloud.plugin.trafficmirroring.TrafficMirrorExceptionPlugin}.
		 */
		public static final int TRAFFIC_MIRRORING_PLUGIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

		/**
		 * order for
		 * {@link com.tencent.cloud.plugin.fault.FaultInjectionPrePlugin}.
		 */
		public static final int FAULT_INJECTION_PLUGIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 40;

		/**
		 * order for
		 * {@link com.tencent.cloud.metadata.core.EncodeTransferMedataFeignEnhancedPlugin}
		 * and
		 * {@link com.tencent.cloud.metadata.core.EncodeTransferMedataScgEnhancedPlugin}
		 * and
		 * {@link com.tencent.cloud.metadata.core.EncodeTransferMedataWebClientEnhancedPlugin}
		 * and
		 * {@link com.tencent.cloud.metadata.core.EncodeTransferMedataZuulEnhancedPlugin}
		 * and
		 * {@link com.tencent.cloud.metadata.core.EncodeTransferMedataRestTemplateEnhancedPlugin}.
		 */
		public static final int CONSUMER_TRANSFER_METADATA_PLUGIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

		/**
		 * order for
		 * {@link com.tencent.cloud.plugin.trace.TraceMetadataEnhancedPlugin}.
		 */
		public static final int CONSUMER_TRACE_METADATA_PLUGIN_ORDER = CONSUMER_TRANSFER_METADATA_PLUGIN_ORDER - 1;

		/**
		 * order for
		 * {@link com.tencent.cloud.plugin.trace.TraceClientPreEnhancedPlugin}
		 * and
		 * {@link com.tencent.cloud.plugin.trace.TraceClientFinallyEnhancedPlugin}.
		 */
		public static final int TRACE_CLIENT_PLUGIN_ORDER = CONSUMER_TRANSFER_METADATA_PLUGIN_ORDER + 3;
	}

	public static class ServerPluginOrder {

		/**
		 * order for
		 * {@link com.tencent.cloud.plugin.trace.TraceServerPreEnhancedPlugin}.
		 */
		public static final int TRACE_SERVER_PRE_PLUGIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 1;
	}

}
