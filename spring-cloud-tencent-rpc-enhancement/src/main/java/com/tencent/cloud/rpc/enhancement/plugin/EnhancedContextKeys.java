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

/**
 * Common key constants stored in {@link EnhancedPluginContext#getExtraData()}.
 *
 * <p>These constants live in the rpc-enhancement module so they can be referenced by both the
 * SCG / WebClient reactive client interceptors and the upper trace-plugin, without creating a
 * module cycle.</p>
 *
 * @author Haotian Zhang
 */
public final class EnhancedContextKeys {

	/**
	 * Pending baggage attributes (Map of string) staged by the trace plugin in the BEFORE_CALLING
	 * stage for reactive clients (SCG / WebClient). The SCG / WebClient interceptor removes them and
	 * injects them as a W3C baggage HTTP header on the outgoing request. No OTel Scope is attached,
	 * so there is no ThreadLocal lifecycle to close across async boundaries.
	 */
	public static final String PENDING_BAGGAGE_ATTRIBUTES_KEY = "PENDING_BAGGAGE_ATTRIBUTES_KEY";

	private EnhancedContextKeys() {
	}
}
