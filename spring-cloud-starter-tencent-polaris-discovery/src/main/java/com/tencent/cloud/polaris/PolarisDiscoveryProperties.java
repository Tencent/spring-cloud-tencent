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

package com.tencent.cloud.polaris;

import com.tencent.cloud.common.constant.ContextConstant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.tencent.cloud.common.constant.ContextConstant.DEFAULT_REGISTRY_HEARTBEAT_TIME_INTERVAL;

/**
 * Properties for Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
@ConfigurationProperties("spring.cloud.polaris.discovery")
public class PolarisDiscoveryProperties {

	/**
	 * Namespace, separation registry of different environments.
	 */
	@Value("${spring.cloud.polaris.discovery.namespace:${spring.cloud.polaris.namespace:#{'default'}}}")
	private String namespace;

	/**
	 * Service name to registry.
	 */
	@Value("${spring.cloud.polaris.discovery.service:${spring.cloud.polaris.service:${spring.application.name:}}}")
	private String service;

	/**
	 * Service instance id.
	 */
	private String instanceId;

	/**
	 * The polaris authentication token.
	 */
	private String token;

	/**
	 * Load balance weight.
	 */
	@Value("${spring.cloud.polaris.discovery.weight:#{100}}")
	private int weight;

	/**
	 * Version number.
	 */
	private String version = "1.0.0";

	/**
	 * Protocol name such as http, https.
	 */
	@Value("${spring.cloud.polaris.discovery.protocol:http}")
	private String protocol;

	/**
	 * Enable polaris discovery or not.
	 */
	private Boolean enabled = true;

	/**
	 * If instance registered.
	 */
	@Value("${spring.cloud.polaris.discovery.register:#{true}}")
	private Boolean registerEnabled;

	/**
	 * Heartbeat interval ( 0 < interval <= 60).
	 * Time unit: second. Default: 5.
	 * @see ContextConstant#DEFAULT_REGISTRY_HEARTBEAT_TIME_INTERVAL
	 */
	private Integer heartbeatInterval = DEFAULT_REGISTRY_HEARTBEAT_TIME_INTERVAL;

	/**
	 * Custom health check url to override default.
	 */
	@Value("${spring.cloud.polaris.discovery.health-check-url:}")
	private String healthCheckUrl;

	/**
	 * Millis interval of refresh of service info list. Default: 60000.
	 */
	private Long serviceListRefreshInterval = 60000L;

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public Boolean isEnabled() {
		return enabled;
	}

	public boolean isRegisterEnabled() {
		return registerEnabled;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getHealthCheckUrl() {
		return healthCheckUrl;
	}

	public void setHealthCheckUrl(String healthCheckUrl) {
		this.healthCheckUrl = healthCheckUrl;
	}

	public Long getServiceListRefreshInterval() {
		return serviceListRefreshInterval;
	}

	public void setServiceListRefreshInterval(Long serviceListRefreshInterval) {
		this.serviceListRefreshInterval = serviceListRefreshInterval;
	}

	public Integer getHeartbeatInterval() {
		if (this.heartbeatInterval <= 0 || this.heartbeatInterval > 60) {
			heartbeatInterval = DEFAULT_REGISTRY_HEARTBEAT_TIME_INTERVAL;
		}
		return heartbeatInterval;
	}

	public void setHeartbeatInterval(Integer heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getRegisterEnabled() {
		return registerEnabled;
	}

	public void setRegisterEnabled(boolean registerEnabled) {
		this.registerEnabled = registerEnabled;
	}

	public void setRegisterEnabled(Boolean registerEnabled) {
		this.registerEnabled = registerEnabled;
	}

	@Override
	public String toString() {
		return "PolarisDiscoveryProperties{" +
				"namespace='" + namespace + '\'' +
				", service='" + service + '\'' +
				", instanceId='" + instanceId + '\'' +
				", token='" + token + '\'' +
				", weight=" + weight +
				", version='" + version + '\'' +
				", protocol='" + protocol + '\'' +
				", enabled=" + enabled +
				", registerEnabled=" + registerEnabled +
				", heartbeatInterval=" + heartbeatInterval +
				", healthCheckUrl='" + healthCheckUrl + '\'' +
				", serviceListRefreshInterval=" + serviceListRefreshInterval +
				'}';
	}
}
