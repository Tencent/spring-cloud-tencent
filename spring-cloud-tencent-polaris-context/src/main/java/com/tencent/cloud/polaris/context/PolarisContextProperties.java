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

package com.tencent.cloud.polaris.context;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.tencent.polaris.api.config.ConfigProvider;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import org.apache.commons.lang.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
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
	private String address;

	/**
	 * current server local ip address.
	 */
	private String localIpAddress;

	/**
	 * polaris namespace.
	 */
	private String namespace = "default";

	/**
	 * polaris service name.
	 */
	private String service;

	@Autowired
	private Environment environment;

	@Autowired
	private List<PolarisConfigModifier> modifierList;

	protected Configuration configuration() {
		ConfigurationImpl configuration = (ConfigurationImpl) ConfigAPIFactory
				.defaultConfig(ConfigProvider.DEFAULT_CONFIG);
		configuration.setDefault();
		String defaultHost = getHost();
		configuration.getGlobal().getAPI().setBindIP(defaultHost);
		Collection<PolarisConfigModifier> modifiers = modifierList;
		modifiers = modifiers.stream().sorted(Comparator.comparingInt(PolarisConfigModifier::getOrder))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(modifiers)) {
			for (PolarisConfigModifier modifier : modifiers) {
				modifier.modify(configuration);
			}
		}
		return configuration;
	}

	private String getHost() {
		if (StringUtils.isNotBlank(localIpAddress)) {
			return localIpAddress;
		}
		return environment.getProperty("spring.cloud.client.ip-address");
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	String getLocalIpAddress() {
		return localIpAddress;
	}

	void setLocalIpAddress(String localIpAddress) {
		this.localIpAddress = localIpAddress;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	String getService() {
		return service;
	}

	void setService(String service) {
		this.service = service;
	}
}
