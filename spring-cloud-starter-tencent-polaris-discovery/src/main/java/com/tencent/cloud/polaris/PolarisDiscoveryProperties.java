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

import javax.annotation.PostConstruct;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.DiscoveryConfigImpl;
import com.tencent.polaris.factory.config.provider.RegisterConfigImpl;
import org.apache.commons.lang.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Properties for Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
@ConfigurationProperties("spring.cloud.polaris.discovery")
public class PolarisDiscoveryProperties {

	/**
	 * The polaris authentication token.
	 */
	private String token;

	/**
	 * Namespace, separation registry of different environments.
	 */
	@Value("${spring.cloud.polaris.discovery.namespace:${spring.cloud.polaris.namespace:#{'default'}}}")
	private String namespace;

	/**
	 * Service name to registry.
	 */
	@Value("${spring.cloud.polaris.discovery.service:${spring.application.name:}}")
	private String service;

	/**
	 * Load balance weight.
	 */
	@Value("${spring.cloud.polaris.discovery.weight:#{100}}")
	private float weight;

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
	@Value("${server.port:}")
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
	 * Custom health check url to override default.
	 */
	@Value("${spring.cloud.polaris.discovery.health-check-url:}")
	private String healthCheckUrl;

	@Autowired
	private Environment environment;

	/**
	 * Init properties.
	 */
	@PostConstruct
	public void init() {
		if (StringUtils.isEmpty(this.getNamespace())) {
			this.setNamespace(environment
					.resolvePlaceholders("${spring.cloud.polaris.discovery.namespace:}"));
		}
		if (StringUtils.isEmpty(this.getService())) {
			this.setService(environment
					.resolvePlaceholders("${spring.cloud.polaris.discovery.service:}"));
		}
		if (StringUtils.isEmpty(this.getToken())) {
			this.setToken(environment
					.resolvePlaceholders("${spring.cloud.polaris.discovery.token:}"));
		}
	}

	public boolean isHeartbeatEnabled() {
		if (null == heartbeatEnabled) {
			return false;
		}
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

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
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

	@Override
	public String toString() {
		return "PolarisProperties{" + "token='" + token + '\'' + ", namespace='"
				+ namespace + '\'' + ", service='" + service + '\'' + ", weight=" + weight
				+ ", version='" + version + '\'' + ", protocol='" + protocol + '\''
				+ ", port=" + port + '\'' + ", registerEnabled=" + registerEnabled
				+ ", heartbeatEnabled=" + heartbeatEnabled + ", healthCheckUrl="
				+ healthCheckUrl + ", environment=" + environment + '}';
	}

	@Bean
	@ConditionalOnMissingBean
	public PolarisDiscoveryConfigModifier polarisDiscoveryConfigModifier() {
		return new PolarisDiscoveryConfigModifier();
	}

	private static class PolarisDiscoveryConfigModifier implements PolarisConfigModifier {

		private final String ID = "polaris";

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
