/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.context.config.extend.consul;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Copy from org.springframework.cloud.consul.ConsulProperties.
 *
 * @author Spencer Gibb
 */
@ConfigurationProperties(ConsulProperties.PREFIX)
public class ConsulProperties {

	/**
	 * Prefix for configuration properties.
	 */
	public static final String PREFIX = "spring.cloud.consul";

	/** Consul agent hostname. Defaults to 'localhost'. */
	private String host = "localhost";

	/**
	 * Consul agent scheme (HTTP/HTTPS). If there is no scheme in address - client
	 * will use HTTP.
	 */
	private String scheme;

	/** Consul agent port. Defaults to '8500'. */
	private int port = 8500;

	/** Is spring cloud consul enabled. */
	private boolean enabled = false;

	@Value("${consul.token:${CONSUL_TOKEN:${spring.cloud.consul.token:${SPRING_CLOUD_CONSUL_TOKEN:}}}}")
	private String aclToken;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public String getAclToken() {
		return aclToken;
	}

	public void setAclToken(String aclToken) {
		this.aclToken = aclToken;
	}

	@Override
	public String toString() {
		return "ConsulProperties{" +
				"host='" + host + '\'' +
				", scheme='" + scheme + '\'' +
				", port=" + port +
				", enabled=" + enabled +
				", aclToken='" + aclToken + '\'' +
				'}';
	}
}
