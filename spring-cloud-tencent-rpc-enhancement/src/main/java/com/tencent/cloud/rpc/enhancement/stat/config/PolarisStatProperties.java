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

package com.tencent.cloud.rpc.enhancement.stat.config;

import java.io.IOException;
import java.net.ServerSocket;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The properties for stat reporter.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties("spring.cloud.polaris.stat")
public class PolarisStatProperties {

	/**
	 * If state reporter enabled.
	 */
	private boolean enabled = true;

	/**
	 * Local host for prometheus to pull.
	 */
	private String host;

	/**
	 * Port for prometheus to pull.
	 */
	private int port = -1;

	/**
	 * Path for prometheus to pull.
	 */
	private String path = "/metrics";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		if (port == -1 && enabled) {
			port = this.getAvailableStatPort();
		}
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	private int getAvailableStatPort() {
		for (int i = 28080; i <= 65535; i++) {
			try {
				new ServerSocket(i).close();
				return i;
			}
			catch (IOException e) {
				// Stepping into here means port of i can not be used.
			}
		}
		throw new RuntimeException("No port (from 28080 to 65535) can be used for stat report");
	}
}
