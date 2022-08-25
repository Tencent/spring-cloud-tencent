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

package com.tencent.cloud.polaris.registry;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.extend.consul.ConsulContextProperties;
import com.tencent.polaris.client.api.SDKContext;
import org.apache.commons.lang.StringUtils;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Registration object of Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng, Palmer.Xu
 */
public class PolarisRegistration implements Registration {

	private final static String METADATA_KEY_IP = "internal-ip";

	private final static String METADATA_KEY_ADDRESS = "internal-address";

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	private final ConsulContextProperties consulContextProperties;

	private final SDKContext polarisContext;

	private final StaticMetadataManager staticMetadataManager;

	private Map<String, String> metadata;

	private final String host;

	public PolarisRegistration(
			PolarisDiscoveryProperties polarisDiscoveryProperties,
			@Nullable ConsulContextProperties consulContextProperties,
			SDKContext context, StaticMetadataManager staticMetadataManager) {
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
		this.consulContextProperties = consulContextProperties;
		this.polarisContext = context;
		this.staticMetadataManager = staticMetadataManager;

		host = polarisContext.getConfig().getGlobal().getAPI().getBindIP();
	}

	@Override
	public String getServiceId() {
		return polarisDiscoveryProperties.getService();
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public int getPort() {
		return polarisDiscoveryProperties.getPort();
	}

	public void setPort(int port) {
		this.polarisDiscoveryProperties.setPort(port);
	}

	@Override
	public boolean isSecure() {
		return StringUtils.equalsIgnoreCase(polarisDiscoveryProperties.getProtocol(), "https");
	}

	@Override
	public URI getUri() {
		return DefaultServiceInstance.getUri(this);
	}

	@Override
	public Map<String, String> getMetadata() {
		if (CollectionUtils.isEmpty(metadata)) {
			Map<String, String> instanceMetadata = new HashMap<>();

			// put internal metadata
			instanceMetadata.put(METADATA_KEY_IP, host);
			instanceMetadata.put(METADATA_KEY_ADDRESS, host + ":" + polarisDiscoveryProperties.getPort());

			instanceMetadata.putAll(staticMetadataManager.getMergedStaticMetadata());

			this.metadata = Collections.unmodifiableMap(instanceMetadata);
		}
		return metadata;
	}

	public PolarisDiscoveryProperties getPolarisProperties() {
		return polarisDiscoveryProperties;
	}

	public boolean isRegisterEnabled() {

		boolean registerEnabled = false;

		if (null != polarisDiscoveryProperties) {
			registerEnabled |= polarisDiscoveryProperties.isRegisterEnabled();
		}
		if (null != consulContextProperties && consulContextProperties.isEnabled()) {
			registerEnabled |= consulContextProperties.isRegister();
		}

		return registerEnabled;
	}

	@Override
	public String toString() {
		return "PolarisRegistration{" +
				"polarisDiscoveryProperties=" + polarisDiscoveryProperties +
				", polarisContext=" + polarisContext +
				", staticMetadataManager=" + staticMetadataManager +
				", metadata=" + metadata +
				'}';
	}
}
