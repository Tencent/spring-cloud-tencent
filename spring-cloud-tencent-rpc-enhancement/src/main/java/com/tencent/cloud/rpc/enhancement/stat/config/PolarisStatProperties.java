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

import org.springframework.beans.factory.annotation.Value;
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
	 * Port for prometheus to pull. 0 for random from 20000 to 65535.
	 */
	private int port = 0;

	/**
	 * Path for prometheus to pull.
	 */
	private String path = "/metrics";

	/**
	 * If state pushGateway reporter enabled.
	 */
	@Value("${spring.cloud.polaris.stat.pushgateway.enabled:#{false}}")
	private boolean pushGatewayEnabled = false;

	/**
	 * PushGateway address.
	 */
	@Value("${spring.cloud.polaris.stat.pushgateway.address:}")
	private String pushGatewayAddress;

	/**
	 * Push metrics interval.
	 * unit: milliseconds default 30s.
	 */
	@Value("${spring.cloud.polaris.stat.pushgateway.push-interval:#{30000}}")
	private Long pushGatewayPushInterval = 30 * 1000L;

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

	public boolean isPushGatewayEnabled() {
		return pushGatewayEnabled;
	}

	public void setPushGatewayEnabled(boolean pushGatewayEnabled) {
		this.pushGatewayEnabled = pushGatewayEnabled;
	}

	public String getPushGatewayAddress() {
		return pushGatewayAddress;
	}

	public void setPushGatewayAddress(String pushGatewayAddress) {
		this.pushGatewayAddress = pushGatewayAddress;
	}

	public Long getPushGatewayPushInterval() {
		return pushGatewayPushInterval;
	}

	public void setPushGatewayPushInterval(Long pushGatewayPushInterval) {
		this.pushGatewayPushInterval = pushGatewayPushInterval;
	}
}
