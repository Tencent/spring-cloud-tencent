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

package com.tencent.cloud.plugin.lossless.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.polaris.lossless")
public class LosslessProperties {

	private boolean enabled = true;

	private int port = 28080;

	private String healthCheckPath;

	private Long delayRegisterInterval;

	private Long healthCheckInterval;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHealthCheckPath() {
		return healthCheckPath;
	}

	public void setHealthCheckPath(String healthCheckPath) {
		this.healthCheckPath = healthCheckPath;
	}

	public Long getDelayRegisterInterval() {
		return delayRegisterInterval;
	}

	public void setDelayRegisterInterval(Long delayRegisterInterval) {
		this.delayRegisterInterval = delayRegisterInterval;
	}

	public Long getHealthCheckInterval() {
		return healthCheckInterval;
	}

	public void setHealthCheckInterval(Long healthCheckInterval) {
		this.healthCheckInterval = healthCheckInterval;
	}
}
