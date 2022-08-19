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

package com.tencent.cloud.plugin.pushgateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The properties for stat pushGateway reporter.
 *
 * @author lingxiao.wlx
 */
@ConfigurationProperties("spring.cloud.polaris.stat.pushgateway")
public class PolarisStatPushGatewayProperties {

	/**
	 * If state pushGateway reporter enabled.
	 */
	private boolean enabled = false;

	/**
	 * PushGateway address.
	 */
	private String address;

	/**
	 * Service for pushGateway.
	 */
	private String service;

	/**
	 * Namespace for pushGateway.
	 */
	private String namespace;

	/**
	 * Push metrics interval.
	 * unit: milliseconds default 30s.
	 */
	private Long pushInterval = 30 * 1000L;

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

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public Long getPushInterval() {
		return pushInterval;
	}

	public void setPushInterval(Long pushInterval) {
		this.pushInterval = pushInterval;
	}
}
