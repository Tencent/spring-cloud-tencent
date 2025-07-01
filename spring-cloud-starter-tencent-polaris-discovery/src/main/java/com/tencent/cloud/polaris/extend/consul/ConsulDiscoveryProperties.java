/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ecwid.consul.v1.ConsistencyMode;
import com.tencent.cloud.common.util.inet.PolarisInetUtils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils.HostInfo;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Copy from org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties.
 * Defines configuration for service discovery and registration.
 *
 * @author Spencer Gibb
 * @author Donnabell Dmello
 * @author Venil Noronha
 * @author Richard Kettelerij
 */
@ConfigurationProperties(ConsulDiscoveryProperties.PREFIX)
public class ConsulDiscoveryProperties {

	/**
	 * Consul discovery properties prefix.
	 */
	public static final String PREFIX = "spring.cloud.consul.discovery";

	protected static final String MANAGEMENT = "management";

	private HostInfo hostInfo;

	/** Tags to use when registering service. */
	private List<String> tags = new ArrayList<>();

	/** Metadata to use when registering service. */
	private Map<String, String> metadata = new LinkedHashMap<>();

	/** Enable tag override for the registered service. */
	private Boolean enableTagOverride;

	/** Is service discovery enabled? */
	private boolean enabled = true;

	/** Tags to use when registering management service. */
	private List<String> managementTags = new ArrayList<>();

	/** Enable tag override for the registered management service. */
	private Boolean managementEnableTagOverride;

	/** Metadata to use when registering management service. */
	private Map<String, String> managementMetadata;

	/** Alternate server path to invoke for health checking. */
	private String healthCheckPath = "/actuator/health";

	/** Custom health check url to override default. */
	private String healthCheckUrl;

	/** Headers to be applied to the Health Check calls. */
	private Map<String, List<String>> healthCheckHeaders = new HashMap<>();

	/** How often to perform the health check (e.g. 10s), defaults to 10s. */
	private String healthCheckInterval = "10s";

	/** Timeout for health check (e.g. 10s). */
	private String healthCheckTimeout;

	/**
	 * Timeout to deregister services critical for longer than timeout (e.g. 30m).
	 * Requires consul version 7.x or higher.
	 */
	private String healthCheckCriticalTimeout;

	/**
	 * IP address to use when accessing service (must also set preferIpAddress to use).
	 */
	private String ipAddress;

	/** Hostname to use when accessing server. */
	private String hostname;

	/** Port to register the service under (defaults to listening port). */
	private Integer port;

	/** Port to register the management service under (defaults to management port). */
	private Integer managementPort;

	private Lifecycle lifecycle = new Lifecycle();

	/** Use ip address rather than hostname during registration. */
	private boolean preferIpAddress = true;

	/** Source of how we will determine the address to use. */
	private boolean preferAgentAddress = false;

	/** The delay between calls to watch consul catalog in millis, default is 1000. */
	private int catalogServicesWatchDelay = 1000;

	/** The number of seconds to block while watching consul catalog, default is 2. */
	private int catalogServicesWatchTimeout = 55;

	/** Service name. */
	private String serviceName;

	/** Unique service instance id. */
	private String instanceId;

	/** Service instance zone. */
	private String instanceZone;

	/** Service instance group. */
	private String instanceGroup;

	/**
	 * Whether hostname is included into the default instance id when registering service.
	 */
	private boolean includeHostnameInInstanceId = false;

	/**
	 * Consistency mode for health service request.
	 */
	private ConsistencyMode consistencyMode = ConsistencyMode.DEFAULT;

	/**
	 * Service instance zone comes from metadata. This allows changing the metadata tag
	 * name.
	 */
	private String defaultZoneMetadataName = "zone";

	/** Whether to register an http or https service. */
	private String scheme = "http";

	/** Suffix to use when registering management service. */
	private String managementSuffix = MANAGEMENT;

	/**
	 * Map of serviceId's -> tag to query for in server list. This allows filtering
	 * services by one more tags. Multiple tags can be specified with a comma separated
	 * value.
	 */
	private Map<String, String> serverListQueryTags = new HashMap<>();

	/**
	 * Map of serviceId's -> datacenter to query for in server list. This allows looking
	 * up services in another datacenters.
	 */
	private Map<String, String> datacenters = new HashMap<>();

	/**
	 * Tag to query for in service list if one is not listed in serverListQueryTags.
	 * Multiple tags can be specified with a comma separated value.
	 */
	private String defaultQueryTag;

	/**
	 * Add the 'passing` parameter to /v1/health/service/serviceName. This pushes health
	 * check passing to the server.
	 */
	private boolean queryPassing = true;

	/** Register as a service in consul. */
	private boolean register = true;

	/** Disable automatic de-registration of service in consul. */
	private boolean deregister = true;

	/** Register health check in consul. Useful during development of a service. */
	private boolean registerHealthCheck = true;

	/**
	 * Throw exceptions during service registration if true, otherwise, log warnings
	 * (defaults to true).
	 */
	private boolean failFast = true;

	/**
	 * Skips certificate verification during service checks if true, otherwise runs
	 * certificate verification.
	 */
	private Boolean healthCheckTlsSkipVerify;

	/**
	 * Order of the discovery client used by `CompositeDiscoveryClient` for sorting
	 * available clients.
	 */
	private int order = 0;

	@SuppressWarnings("unused")
	private ConsulDiscoveryProperties() {
		this(new PolarisInetUtils(new InetUtilsProperties()));
	}

	public ConsulDiscoveryProperties(PolarisInetUtils polarisInetUtils) {
		this.managementTags.add(MANAGEMENT);
		this.hostInfo = polarisInetUtils.findFirstNonLoopbackHostInfo();
		this.ipAddress = this.hostInfo.getIpAddress();
		this.hostname = this.hostInfo.getHostname();
	}

	/**
	 * Gets the tag to use when looking up the instances for a particular service. If the
	 * service has an entry in {@link #serverListQueryTags} that will be used. Otherwise
	 * the content of {@link #defaultQueryTag} will be used.
	 * @param serviceId the service whose instances are being looked up
	 * @return the tag to filter the service instances by or null if no tags are
	 * configured for the service and the default query tag is not configured
	 */
	public String getQueryTagForService(String serviceId) {
		String tag = this.serverListQueryTags.get(serviceId);
		return tag != null ? tag : this.defaultQueryTag;
	}

	/**
	 * Gets the array of tags to use when looking up the instances for a particular
	 * service. If the service has an entry in {@link #serverListQueryTags} that will be
	 * used. Otherwise the content of {@link #defaultQueryTag} will be used. This differs
	 * from {@link #getQueryTagForService(String)} in that it assumes the configured tag
	 * property value may represent multiple tags when separated by commas. When the tag
	 * property is set to a single tag then this method behaves identical to its
	 * aforementioned counterpart except that it returns a single element array with the
	 * single tag value.
	 * <p>
	 * The expected format of the tag property value is {@code tag1,tag2,..,tagN}.
	 * Whitespace will be trimmed off each entry.
	 * @param serviceId the service whose instances are being looked up
	 * @return the array of tags to filter the service instances by - it will be null if
	 * no tags are configured for the service and the default query tag is not configured
	 * or if a single tag is configured and it is the empty string
	 */
	@Nullable
	public String[] getQueryTagsForService(String serviceId) {
		String queryTagStr = getQueryTagForService(serviceId);
		if (queryTagStr == null || queryTagStr.isEmpty()) {
			return null;
		}
		return StringUtils.tokenizeToStringArray(queryTagStr, ",");
	}

	public String getHostname() {
		return this.preferIpAddress ? this.ipAddress : this.hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
		this.hostInfo.override = true;
	}

	private HostInfo getHostInfo() {
		return this.hostInfo;
	}

	private void setHostInfo(HostInfo hostInfo) {
		this.hostInfo = hostInfo;
	}

	public List<String> getTags() {
		return this.tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public boolean isEnableTagOverride() {
		return enableTagOverride;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<String> getManagementTags() {
		return this.managementTags;
	}

	public void setManagementTags(List<String> managementTags) {
		this.managementTags = managementTags;
	}

	public String getHealthCheckPath() {
		return this.healthCheckPath;
	}

	public void setHealthCheckPath(String healthCheckPath) {
		this.healthCheckPath = healthCheckPath;
	}

	public String getHealthCheckUrl() {
		return this.healthCheckUrl;
	}

	public void setHealthCheckUrl(String healthCheckUrl) {
		this.healthCheckUrl = healthCheckUrl;
	}

	public Map<String, List<String>> getHealthCheckHeaders() {
		return this.healthCheckHeaders;
	}

	public void setHealthCheckHeaders(Map<String, List<String>> healthCheckHeaders) {
		this.healthCheckHeaders = healthCheckHeaders;
	}

	public String getHealthCheckInterval() {
		return this.healthCheckInterval;
	}

	public void setHealthCheckInterval(String healthCheckInterval) {
		this.healthCheckInterval = healthCheckInterval;
	}

	public String getHealthCheckTimeout() {
		return this.healthCheckTimeout;
	}

	public void setHealthCheckTimeout(String healthCheckTimeout) {
		this.healthCheckTimeout = healthCheckTimeout;
	}

	public String getHealthCheckCriticalTimeout() {
		return this.healthCheckCriticalTimeout;
	}

	public void setHealthCheckCriticalTimeout(String healthCheckCriticalTimeout) {
		this.healthCheckCriticalTimeout = healthCheckCriticalTimeout;
	}

	public String getIpAddress() {
		return this.ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
		this.hostInfo.override = true;
	}

	public Integer getPort() {
		return this.port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Integer getManagementPort() {
		return this.managementPort;
	}

	public void setManagementPort(Integer managementPort) {
		this.managementPort = managementPort;
	}

	public Lifecycle getLifecycle() {
		return this.lifecycle;
	}

	public void setLifecycle(Lifecycle lifecycle) {
		this.lifecycle = lifecycle;
	}

	public boolean isPreferIpAddress() {
		return this.preferIpAddress;
	}

	public void setPreferIpAddress(boolean preferIpAddress) {
		this.preferIpAddress = preferIpAddress;
	}

	public boolean isPreferAgentAddress() {
		return this.preferAgentAddress;
	}

	public void setPreferAgentAddress(boolean preferAgentAddress) {
		this.preferAgentAddress = preferAgentAddress;
	}

	public int getCatalogServicesWatchDelay() {
		return this.catalogServicesWatchDelay;
	}

	public void setCatalogServicesWatchDelay(int catalogServicesWatchDelay) {
		this.catalogServicesWatchDelay = catalogServicesWatchDelay;
	}

	public int getCatalogServicesWatchTimeout() {
		return this.catalogServicesWatchTimeout;
	}

	public void setCatalogServicesWatchTimeout(int catalogServicesWatchTimeout) {
		this.catalogServicesWatchTimeout = catalogServicesWatchTimeout;
	}

	public String getServiceName() {
		return this.serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getInstanceId() {
		return this.instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getInstanceZone() {
		return this.instanceZone;
	}

	public void setInstanceZone(String instanceZone) {
		this.instanceZone = instanceZone;
	}

	public String getInstanceGroup() {
		return this.instanceGroup;
	}

	public void setInstanceGroup(String instanceGroup) {
		this.instanceGroup = instanceGroup;
	}

	public boolean isIncludeHostnameInInstanceId() {
		return includeHostnameInInstanceId;
	}

	public void setIncludeHostnameInInstanceId(boolean includeHostnameInInstanceId) {
		this.includeHostnameInInstanceId = includeHostnameInInstanceId;
	}

	public ConsistencyMode getConsistencyMode() {
		return consistencyMode;
	}

	public void setConsistencyMode(ConsistencyMode consistencyMode) {
		this.consistencyMode = consistencyMode;
	}

	public String getDefaultZoneMetadataName() {
		return this.defaultZoneMetadataName;
	}

	public void setDefaultZoneMetadataName(String defaultZoneMetadataName) {
		this.defaultZoneMetadataName = defaultZoneMetadataName;
	}

	public String getScheme() {
		return this.scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public String getManagementSuffix() {
		return this.managementSuffix;
	}

	public void setManagementSuffix(String managementSuffix) {
		this.managementSuffix = managementSuffix;
	}

	public Map<String, String> getServerListQueryTags() {
		return this.serverListQueryTags;
	}

	public void setServerListQueryTags(Map<String, String> serverListQueryTags) {
		this.serverListQueryTags = serverListQueryTags;
	}

	public Map<String, String> getDatacenters() {
		return this.datacenters;
	}

	public void setDatacenters(Map<String, String> datacenters) {
		this.datacenters = datacenters;
	}

	public String getDefaultQueryTag() {
		return this.defaultQueryTag;
	}

	public void setDefaultQueryTag(String defaultQueryTag) {
		this.defaultQueryTag = defaultQueryTag;
	}

	public boolean isQueryPassing() {
		return this.queryPassing;
	}

	public void setQueryPassing(boolean queryPassing) {
		this.queryPassing = queryPassing;
	}

	public boolean isRegister() {
		return this.register;
	}

	public void setRegister(boolean register) {
		this.register = register;
	}

	public boolean isDeregister() {
		return this.deregister;
	}

	public void setDeregister(boolean deregister) {
		this.deregister = deregister;
	}

	public boolean isRegisterHealthCheck() {
		return this.registerHealthCheck;
	}

	public void setRegisterHealthCheck(boolean registerHealthCheck) {
		this.registerHealthCheck = registerHealthCheck;
	}

	public boolean isFailFast() {
		return this.failFast;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	public Boolean getHealthCheckTlsSkipVerify() {
		return this.healthCheckTlsSkipVerify;
	}

	public void setHealthCheckTlsSkipVerify(Boolean healthCheckTlsSkipVerify) {
		this.healthCheckTlsSkipVerify = healthCheckTlsSkipVerify;
	}

	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public Map<String, String> getManagementMetadata() {
		return this.managementMetadata;
	}

	public void setManagementMetadata(Map<String, String> managementMetadata) {
		this.managementMetadata = managementMetadata;
	}

	public Boolean getEnableTagOverride() {
		return this.enableTagOverride;
	}

	public void setEnableTagOverride(boolean enableTagOverride) {
		this.enableTagOverride = enableTagOverride;
	}

	public void setEnableTagOverride(Boolean enableTagOverride) {
		this.enableTagOverride = enableTagOverride;
	}

	public Boolean getManagementEnableTagOverride() {
		return this.managementEnableTagOverride;
	}

	public void setManagementEnableTagOverride(Boolean managementEnableTagOverride) {
		this.managementEnableTagOverride = managementEnableTagOverride;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("catalogServicesWatchDelay", this.catalogServicesWatchDelay)
				.append("catalogServicesWatchTimeout", this.catalogServicesWatchTimeout)
				.append("consistencyMode", this.consistencyMode)
				.append("datacenters", this.datacenters)
				.append("defaultQueryTag", this.defaultQueryTag)
				.append("defaultZoneMetadataName", this.defaultZoneMetadataName)
				.append("deregister", this.deregister)
				.append("enabled", this.enabled)
				.append("enableTagOverride", this.enableTagOverride)
				.append("failFast", this.failFast)
				.append("hostInfo", this.hostInfo)
				.append("healthCheckCriticalTimeout", this.healthCheckCriticalTimeout)
				.append("healthCheckHeaders", this.healthCheckHeaders)
				.append("healthCheckInterval", this.healthCheckInterval)
				.append("healthCheckPath", this.healthCheckPath)
				.append("healthCheckTimeout", this.healthCheckTimeout)
				.append("healthCheckTlsSkipVerify", this.healthCheckTlsSkipVerify)
				.append("healthCheckUrl", this.healthCheckUrl)
				.append("hostname", this.hostname)
				.append("includeHostnameInInstanceId", this.includeHostnameInInstanceId)
				.append("instanceId", this.instanceId)
				.append("instanceGroup", this.instanceGroup)
				.append("instanceZone", this.instanceZone)
				.append("ipAddress", this.ipAddress)
				.append("lifecycle", this.lifecycle)
				.append("metadata", this.metadata)
				.append("managementEnableTagOverride", this.managementEnableTagOverride)
				.append("managementMetadata", this.managementMetadata)
				.append("managementPort", this.managementPort)
				.append("managementSuffix", this.managementSuffix)
				.append("managementTags", this.managementTags)
				.append("order", this.order)
				.append("port", this.port)
				.append("preferAgentAddress", this.preferAgentAddress)
				.append("preferIpAddress", this.preferIpAddress)
				.append("queryPassing", this.queryPassing)
				.append("register", this.register)
				.append("registerHealthCheck", this.registerHealthCheck)
				.append("scheme", this.scheme)
				.append("serviceName", this.serviceName)
				.append("serverListQueryTags", this.serverListQueryTags)
				.append("tags", this.tags)
				.toString();
	}

	/**
	 * Properties releated to the lifecycle.
	 */
	public static class Lifecycle {

		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public String toString() {
			return "Lifecycle{" + "enabled=" + this.enabled + '}';
		}

	}
}
