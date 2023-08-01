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

package com.tencent.cloud.polaris.registry;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.extend.consul.ConsulContextProperties;
import com.tencent.cloud.polaris.extend.nacos.NacosContextProperties;
import com.tencent.polaris.client.api.SDKContext;
import org.apache.commons.lang.StringUtils;

import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import static com.tencent.cloud.polaris.extend.nacos.NacosContextProperties.DEFAULT_CLUSTER;
import static com.tencent.cloud.polaris.extend.nacos.NacosContextProperties.DEFAULT_GROUP;

/**
 * Registration object of Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng, Palmer.Xu, changjin wei(魏昌进)
 */
public class PolarisRegistration implements Registration {

	private static final String METADATA_KEY_IP = "internal-ip";
	private static final String METADATA_KEY_ADDRESS = "internal-address";
	private static final String GROUP_SERVER_ID_FORMAT = "%s__%s";
	private static final String NACOS_CLUSTER = "nacos.cluster";

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	private final SDKContext polarisContext;

	private final StaticMetadataManager staticMetadataManager;

	private final String serviceId;
	private final String host;
	private final boolean isSecure;
	private final ServletWebServerApplicationContext servletWebServerApplicationContext;
	private final ReactiveWebServerApplicationContext reactiveWebServerApplicationContext;
	private final List<PolarisRegistrationCustomizer> customizers;
	private boolean registerEnabled = false;
	private Map<String, String> metadata;
	private int port;
	private String instanceId;

	public PolarisRegistration(
			PolarisDiscoveryProperties polarisDiscoveryProperties,
			@Nullable PolarisContextProperties polarisContextProperties,
			@Nullable ConsulContextProperties consulContextProperties,
			SDKContext context, StaticMetadataManager staticMetadataManager,
			@Nullable NacosContextProperties nacosContextProperties,
			@Nullable ServletWebServerApplicationContext servletWebServerApplicationContext,
			@Nullable ReactiveWebServerApplicationContext reactiveWebServerApplicationContext,
			@Nullable List<PolarisRegistrationCustomizer> registrationCustomizers) {
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
		this.polarisContext = context;
		this.staticMetadataManager = staticMetadataManager;
		this.servletWebServerApplicationContext = servletWebServerApplicationContext;
		this.reactiveWebServerApplicationContext = reactiveWebServerApplicationContext;
		this.customizers = registrationCustomizers;

		// generate serviceId
		if (Objects.isNull(nacosContextProperties)) {
			serviceId = polarisDiscoveryProperties.getService();
		}
		else {
			String group = nacosContextProperties.getGroup();
			if (StringUtils.isNotBlank(group) && !DEFAULT_GROUP.equals(group)) {
				serviceId = String.format(GROUP_SERVER_ID_FORMAT, group, polarisDiscoveryProperties.getService());
			}
			else {
				serviceId = polarisDiscoveryProperties.getService();
			}
		}

		// generate host
		host = polarisContext.getConfig().getGlobal().getAPI().getBindIP();

		// generate port
		if (polarisContextProperties != null) {
			port = polarisContextProperties.getLocalPort();
		}

		// generate isSecure
		isSecure = StringUtils.equalsIgnoreCase(polarisDiscoveryProperties.getProtocol(), "https");

		// generate metadata
		if (CollectionUtils.isEmpty(metadata)) {
			Map<String, String> instanceMetadata = new HashMap<>();

			// put internal metadata
			instanceMetadata.put(METADATA_KEY_IP, host);
			instanceMetadata.put(METADATA_KEY_ADDRESS, host + ":" + port);

			// put internal-nacos-cluster if necessary
			if (Objects.nonNull(nacosContextProperties)) {
				String clusterName = nacosContextProperties.getClusterName();
				if (StringUtils.isNotBlank(clusterName) && !DEFAULT_CLUSTER.equals(clusterName)) {
					instanceMetadata.put(NACOS_CLUSTER, clusterName);
				}
			}

			instanceMetadata.putAll(staticMetadataManager.getMergedStaticMetadata());

			this.metadata = instanceMetadata;
		}

		// generate registerEnabled
		if (null != polarisDiscoveryProperties) {
			registerEnabled = polarisDiscoveryProperties.isRegisterEnabled();
		}
		if (null != consulContextProperties && consulContextProperties.isEnabled()) {
			registerEnabled |= consulContextProperties.isRegister();
		}
		if (null != nacosContextProperties && nacosContextProperties.isEnabled()) {
			registerEnabled |= nacosContextProperties.isRegisterEnabled();
		}
	}

	public static PolarisRegistration registration(PolarisDiscoveryProperties polarisDiscoveryProperties,
			@Nullable PolarisContextProperties polarisContextProperties,
			@Nullable ConsulContextProperties consulContextProperties,
			SDKContext context, StaticMetadataManager staticMetadataManager,
			@Nullable NacosContextProperties nacosContextProperties,
			@Nullable ServletWebServerApplicationContext servletWebServerApplicationContext,
			@Nullable ReactiveWebServerApplicationContext reactiveWebServerApplicationContext,
			@Nullable List<PolarisRegistrationCustomizer> registrationCustomizers) {
		PolarisRegistration polarisRegistration = new PolarisRegistration(polarisDiscoveryProperties,
				polarisContextProperties, consulContextProperties, context, staticMetadataManager,
				nacosContextProperties, servletWebServerApplicationContext, reactiveWebServerApplicationContext,
				registrationCustomizers);
		return polarisRegistration;
	}

	public void customize() {
		if (!CollectionUtils.isEmpty(this.customizers)) {
			for (PolarisRegistrationCustomizer customizer : this.customizers) {
				customizer.customize(this);
			}
		}
	}

	@Override
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String getHost() {
		return host;
	}

	/**
	 * Should be call after web started.
	 *
	 * @return port
	 */
	@Override
	public int getPort() {
		if (port <= 0) {
			if (servletWebServerApplicationContext != null) {
				port = servletWebServerApplicationContext.getWebServer().getPort();
			}
			else if (reactiveWebServerApplicationContext != null) {
				port = reactiveWebServerApplicationContext.getWebServer().getPort();
			}
			else {
				throw new RuntimeException("Unsupported web type.");
			}
		}
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public boolean isSecure() {
		return isSecure;
	}

	@Override
	public URI getUri() {
		return DefaultServiceInstance.getUri(this);
	}

	@Override
	public Map<String, String> getMetadata() {
		return metadata;
	}

	@Override
	public String getInstanceId() {
		return instanceId;
	}

	protected void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public boolean isRegisterEnabled() {
		return registerEnabled;
	}

	@Override
	public String toString() {
		return "PolarisRegistration{" +
				" polarisDiscoveryProperties=" + polarisDiscoveryProperties +
				", polarisContext=" + polarisContext +
				", staticMetadataManager=" + staticMetadataManager +
				", metadata=" + metadata +
				", host='" + host + '\'' +
				", instanceId='" + instanceId + '\'' +
				'}';
	}
}
