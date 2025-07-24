/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
