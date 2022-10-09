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

package com.tencent.cloud.polaris.config.config;

import java.util.List;

import com.tencent.cloud.polaris.config.enums.RefreshType;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * polaris config module bootstrap configs.
 *
 * @author lepdou 2022-03-10
 */
@ConfigurationProperties("spring.cloud.polaris.config")
public class PolarisConfigProperties {

	/**
	 * Whether to open the configuration center.
	 */
	private boolean enabled = true;

	/**
	 * Configuration center service address list.
	 */
	private String address;

	/**
	 * Polaris config grpc port.
	 */
	private int port = 8093;

	/**
	 * Whether to automatically update to the spring context when the configuration file.
	 * is updated
	 */
	private boolean autoRefresh = true;

	private boolean shutdownIfConnectToConfigServerFailed = true;

	/**
	 * Attribute refresh type.
	 */
	private RefreshType refreshType = RefreshType.REFLECT;

	/**
	 * List of injected configuration files.
	 */
	private List<ConfigFileGroup> groups;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isAutoRefresh() {
		return autoRefresh;
	}

	public void setAutoRefresh(boolean autoRefresh) {
		this.autoRefresh = autoRefresh;
	}

	public boolean isShutdownIfConnectToConfigServerFailed() {
		return shutdownIfConnectToConfigServerFailed;
	}

	public void setShutdownIfConnectToConfigServerFailed(boolean shutdownIfConnectToConfigServerFailed) {
		this.shutdownIfConnectToConfigServerFailed = shutdownIfConnectToConfigServerFailed;
	}

	public RefreshType getRefreshType() {
		return refreshType;
	}

	public void setRefreshType(RefreshType refreshType) {
		this.refreshType = refreshType;
	}

	public List<ConfigFileGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<ConfigFileGroup> groups) {
		this.groups = groups;
	}
}
