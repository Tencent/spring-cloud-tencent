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

package com.tencent.cloud.polaris.extend.consul;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Discovery configuration of Consul.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties("spring.cloud.consul")
public class ConsulContextProperties {

	/**
	 * Host of consul(or consul agent).
	 */
	private String host;

	private int port;

	private boolean enabled = false;

	@Value("${spring.cloud.consul.discovery.register:#{'true'}}")
	private boolean register;

	@Value("${spring.cloud.consul.discovery.enabled:#{'true'}}")
	private boolean discoveryEnabled;

	@Value("${spring.cloud.consul.discovery.instance-id:}")
	private String instanceId;

	@Value("${spring.cloud.consul.discovery.service-name:${spring.application.name:}}")
	private String serviceName;

	@Value("${spring.cloud.consul.discovery.ip-address:}")
	private String ipAddress;

	@Value("${spring.cloud.consul.discovery.prefer-ip-address:#{'false'}}")
	private boolean preferIpAddress;

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

	public boolean isRegister() {
		return register;
	}

	public boolean isDiscoveryEnabled() {
		return discoveryEnabled;
	}

	public void setRegister(boolean register) {
		this.register = register;
	}

	public void setDiscoveryEnabled(boolean discoveryEnabled) {
		this.discoveryEnabled = discoveryEnabled;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public boolean isPreferIpAddress() {
		return preferIpAddress;
	}

	public void setPreferIpAddress(boolean preferIpAddress) {
		this.preferIpAddress = preferIpAddress;
	}
}
