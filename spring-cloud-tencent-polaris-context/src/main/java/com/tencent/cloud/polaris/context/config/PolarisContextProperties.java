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

package com.tencent.cloud.polaris.context.config;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.api.config.ConfigProvider;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import org.apache.commons.lang.StringUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;

/**
 * Properties for Polaris {@link com.tencent.polaris.client.api.SDKContext}.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties(prefix = "spring.cloud.polaris")
public class PolarisContextProperties {

	/**
	 * polaris server address.
	 */
	@Value("${spring.cloud.polaris.address:}")
	private String address;

	/**
	 * current server local ip address.
	 */
	@Value("${spring.cloud.polaris.localIpAddress:}")
	private String localIpAddress;

	/**
	 * current server local port.
	 */
	@Value("${spring.cloud.polaris.localPort:}")
	private Integer localPort;

	/**
	 * If polaris enabled.
	 */
	@Value("${spring.cloud.polaris.enabled:#{'true'}}")
	private Boolean enabled;

	/**
	 * polaris namespace.
	 */
	@Value("${spring.cloud.polaris.namespace:#{'default'}}")
	private String namespace = "default";

	/**
	 * polaris service name.
	 */
	private String service;

	public Configuration configuration(List<PolarisConfigModifier> modifierList, Supplier<String> ipAddressSupplier, Supplier<Integer> portSupplier) {
		// 1. Read user-defined polaris.yml configuration
		ConfigurationImpl configuration = (ConfigurationImpl) ConfigAPIFactory
				.defaultConfig(ConfigProvider.DEFAULT_CONFIG);

		// 2. Override user-defined polaris.yml configuration with SCT configuration
		String defaultHost = this.localIpAddress;
		if (StringUtils.isBlank(localIpAddress)) {
			defaultHost = ipAddressSupplier.get();
			this.localIpAddress = defaultHost;
		}
		if (this.localPort == null || this.localPort <= 0) {
			this.localPort = portSupplier.get();
		}

		configuration.getGlobal().getAPI().setBindIP(defaultHost);

		Collection<PolarisConfigModifier> modifiers = modifierList;
		modifiers = modifiers.stream()
				.sorted(Comparator.comparingInt(PolarisConfigModifier::getOrder))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(modifiers)) {
			for (PolarisConfigModifier modifier : modifiers) {
				modifier.modify(configuration);
			}
		}

		return configuration;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getLocalIpAddress() {
		return localIpAddress;
	}

	public void setLocalIpAddress(String localIpAddress) {
		this.localIpAddress = localIpAddress;
	}

	public Integer getLocalPort() {
		return localPort;
	}

	public void setLocalPort(Integer localPort) {
		this.localPort = localPort;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	@Override
	public String toString() {
		return "PolarisContextProperties{" +
				"address='" + address + '\'' +
				", localIpAddress='" + localIpAddress + '\'' +
				((this.localPort == null || this.localPort <= 0) ? "" : ", localPort=" + localPort) +
				", enabled=" + enabled +
				", namespace='" + namespace + '\'' +
				((StringUtils.isBlank(this.service)) ? "" : ", service='" + service + '\'') +
				'}';
	}
}
