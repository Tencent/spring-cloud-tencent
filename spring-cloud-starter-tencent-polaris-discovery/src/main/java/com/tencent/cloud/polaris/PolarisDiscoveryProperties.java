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
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.DiscoveryConfigImpl;
import com.tencent.polaris.factory.config.provider.RegisterConfigImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
	private String version;

	/**
	 * Protocol name such as http, https.
	 */
	@Value("${spring.cloud.polaris.discovery.protocol:http}")
	private String protocol;

	/**
	 * Port of instance.
	 */
	@Value("${server.port:8080}")
	private int port;

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
	 * If heartbeat enabled.
	 */
	@Value("${spring.cloud.polaris.discovery.heartbeat.enabled:#{true}}")
	private Boolean heartbeatEnabled = true;

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

	public boolean isHeartbeatEnabled() {
		return heartbeatEnabled;
	}

	public void setHeartbeatEnabled(Boolean heartbeatEnabled) {
		this.heartbeatEnabled = heartbeatEnabled;
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

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isRegisterEnabled() {
		return registerEnabled;
	}

	public void setRegisterEnabled(boolean registerEnabled) {
		this.registerEnabled = registerEnabled;
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

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
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
		if (this.heartbeatEnabled && (this.heartbeatInterval <= 0 || this.heartbeatInterval > 60)) {
			heartbeatInterval = DEFAULT_REGISTRY_HEARTBEAT_TIME_INTERVAL;
		}
		return heartbeatInterval;
	}

	public void setHeartbeatInterval(Integer heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	@Override
	public String toString() {
		return "PolarisDiscoveryProperties{" +
				"namespace='" + namespace + '\'' +
				", service='" + service + '\'' +
				", token='" + token + '\'' +
				", weight=" + weight +
				", version='" + version + '\'' +
				", protocol='" + protocol + '\'' +
				", port=" + port +
				", enabled=" + enabled +
				", registerEnabled=" + registerEnabled +
				", heartbeatEnabled=" + heartbeatEnabled +
				", heartbeatInterval=" + heartbeatInterval +
				", healthCheckUrl='" + healthCheckUrl + '\'' +
				", serviceListRefreshInterval=" + serviceListRefreshInterval +
				'}';
	}

	@Bean
	@ConditionalOnMissingBean
	public PolarisDiscoveryConfigModifier polarisDiscoveryConfigModifier() {
		return new PolarisDiscoveryConfigModifier();
	}

	private static class PolarisDiscoveryConfigModifier implements PolarisConfigModifier {

		private static final String ID = "polaris";

		@Autowired(required = false)
		private PolarisDiscoveryProperties polarisDiscoveryProperties;

		@Override
		public void modify(ConfigurationImpl configuration) {
			if (polarisDiscoveryProperties != null) {
				DiscoveryConfigImpl discoveryConfig = new DiscoveryConfigImpl();
				discoveryConfig.setServerConnectorId(ID);
				discoveryConfig.setEnable(polarisDiscoveryProperties.enabled);
				configuration.getConsumer().getDiscoveries().add(discoveryConfig);

				RegisterConfigImpl registerConfig = new RegisterConfigImpl();
				registerConfig.setServerConnectorId(ID);
				registerConfig.setEnable(polarisDiscoveryProperties.registerEnabled);
				configuration.getProvider().getRegisters().add(registerConfig);
			}
		}

		@Override
		public int getOrder() {
			return ContextConstant.ModifierOrder.LAST;
		}
	}
}
