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

package com.tencent.tsf.gateway.core.constant;

import java.util.Optional;

import com.tencent.tsf.gateway.core.exception.TsfGatewayError;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;

/**
 * @author: vmershen
 * @since: 1.1.0
 **/
public final class PluginConstants {
	/**
	 * 设置每个标签插件JSON串长度.
	 */
	public static final Integer TAG_PLUGIN_INFO_LIST_LIMIT = 3000;

	private PluginConstants() {

	}

	/**
	 * TAG插件traceIdEnabled类型.
	 */
	public enum TraceIdEnabledType {
		/**
		 * Y: enable.
		 */
		Y,
		/**
		 * N: disable.
		 */
		N;

		public static TraceIdEnabledType getTraceIdEnabledType(String enabledType) {
			for (TraceIdEnabledType taskFlowEdgeType : TraceIdEnabledType.values()) {
				if (taskFlowEdgeType.name().equalsIgnoreCase(enabledType)) {
					return taskFlowEdgeType;
				}
			}
			return null;
		}


		public static void checkValidity(String enabledType) {
			TraceIdEnabledType traceIdEnabledType = getTraceIdEnabledType(enabledType);
			Optional.ofNullable(traceIdEnabledType).orElseThrow(()
					-> new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_INVALID, "Tag插件TraceIdEnabled类型"));
		}
	}
}
