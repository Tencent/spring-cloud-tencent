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

import com.tencent.polaris.api.utils.IPAddressUtils;

public final class Env {

	private Env() {
	}

	private final static String consulToken;

	private final static String consulHost;

	private final static Integer consulPort;

	private final static String namespaceId;

	static {
		// 只支持从环境变量取
		consulHost = IPAddressUtils.getIpCompatible(getSystemProperty("tsf_consul_ip", "localhost"));
		consulPort = Integer.parseInt(getSystemProperty("tsf_consul_port", "8500"));
		consulToken = getSystemProperty("tsf_token", "");
		namespaceId = getSystemProperty("tsf_namespace_id", "");
	}

	private static String getSystemProperty(String name, String defaultValue) {
		String val = null;
		if (System.getenv(name) != null) {
			val = System.getenv(name);
		}
		if (System.getProperty(name) != null) {
			val = System.getProperty(name);
		}
		return (val == null) ? defaultValue : val;
	}

	public static String getConsulToken() {
		return consulToken;
	}

	public static String getConsulHost() {
		return consulHost;
	}

	public static Integer getConsulPort() {
		return consulPort;
	}

	public static String getNamespaceId() {
		return namespaceId;
	}
}
