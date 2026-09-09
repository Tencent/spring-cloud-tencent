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

package com.tencent.cloud.polaris.config.endpoint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.polaris.config.PolarisConfigSDKContextManager;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySource;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.config.ConfigFileGroup;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.internal.CompositeConfigFile;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * Endpoint of polaris config.
 *
 * @author shuiqingliu
 **/
@Endpoint(id = "polarisconfig")
public class PolarisConfigEndpoint {

	private final PolarisConfigProperties polarisConfigProperties;

	public PolarisConfigEndpoint(PolarisConfigProperties polarisConfigProperties) {
		this.polarisConfigProperties = polarisConfigProperties;
	}

	@ReadOperation
	public Map<String, Object> polarisConfig() {
		Map<String, Object> configInfo = new LinkedHashMap<>();
		configInfo.put("PolarisConfigProperties", configProperties());
		configInfo.put("PolarisPropertySource", propertySources());
		configInfo.put("ClientId", getClientId());
		return configInfo;
	}

	private Map<String, Object> configProperties() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("enabled", polarisConfigProperties.isEnabled());
		properties.put("address", polarisConfigProperties.getAddress());
		properties.put("port", polarisConfigProperties.getPort());
		properties.put("autoRefresh", polarisConfigProperties.isAutoRefresh());
		properties.put("shutdownIfConnectToConfigServerFailed",
				polarisConfigProperties.isShutdownIfConnectToConfigServerFailed());
		properties.put("preference", polarisConfigProperties.isPreference());
		properties.put("refreshType", polarisConfigProperties.getRefreshType());
		properties.put("groups", configFileGroups(polarisConfigProperties.getGroups()));
		properties.put("dataSource", polarisConfigProperties.getDataSource());
		properties.put("localFileRootPath", polarisConfigProperties.getLocalFileRootPath());
		properties.put("internalEnabled", polarisConfigProperties.isInternalEnabled());
		properties.put("checkAddress", polarisConfigProperties.isCheckAddress());
		properties.put("emptyProtectionEnabled", polarisConfigProperties.isEmptyProtectionEnabled());
		properties.put("emptyProtectionExpiredInterval", polarisConfigProperties.getEmptyProtectionExpiredInterval());

		PolarisConfigProperties.Report reportProperties = polarisConfigProperties.getReport();
		if (reportProperties != null) {
			Map<String, Object> report = new LinkedHashMap<>();
			report.put("enabled", reportProperties.isEnabled());
			if (reportProperties.getEffective() != null) {
				Map<String, Object> effective = new LinkedHashMap<>();
				effective.put("enabled", reportProperties.getEffective().isEnabled());
				report.put("effective", effective);
			}
			properties.put("report", report);
		}
		// token is intentionally omitted: an actuator endpoint must not expose credentials.
		return properties;
	}

	private List<Map<String, Object>> configFileGroups(List<ConfigFileGroup> groups) {
		List<Map<String, Object>> groupInfo = new ArrayList<>();
		if (groups == null) {
			return groupInfo;
		}
		for (ConfigFileGroup group : groups) {
			Map<String, Object> info = new LinkedHashMap<>();
			info.put("namespace", group.getNamespace());
			info.put("name", group.getName());
			info.put("files", group.getFiles() == null ? new ArrayList<>() : new ArrayList<>(group.getFiles()));
			groupInfo.add(info);
		}
		return groupInfo;
	}

	private List<Map<String, Object>> propertySources() {
		List<Map<String, Object>> sources = new ArrayList<>();
		for (PolarisPropertySource source : PolarisPropertySourceManager.getAllPropertySources()) {
			Map<String, Object> sourceInfo = new LinkedHashMap<>();
			sourceInfo.put("namespace", source.getNamespace());
			sourceInfo.put("group", source.getGroup());
			sourceInfo.put("fileName", source.getFileName());
			sourceInfo.put("propertyNames", new ArrayList<>(source.getSource().keySet()));
			sourceInfo.put("configKVFile", configFileInfo(source.getConfigKVFile()));
			sources.add(sourceInfo);
		}
		return sources;
	}

	private Map<String, Object> configFileInfo(ConfigKVFile configFile) {
		Map<String, Object> info = new LinkedHashMap<>();
		if (configFile == null) {
			return info;
		}
		info.put("namespace", configFile.getNamespace());
		info.put("fileGroup", configFile.getFileGroup());
		info.put("fileName", configFile.getFileName());
		info.put("fileVersion", configFile.getFileVersion());
		info.put("propertyNames", configFile.getPropertyNames() == null
				? new ArrayList<>() : new ArrayList<>(configFile.getPropertyNames()));
		if (configFile instanceof CompositeConfigFile) {
			List<Map<String, Object>> files = new ArrayList<>();
			List<ConfigKVFile> configFiles = ((CompositeConfigFile) configFile).getConfigKVFiles();
			if (configFiles != null) {
				for (ConfigKVFile file : configFiles) {
					files.add(configFileInfo(file));
				}
			}
			info.put("configKVFiles", files);
		}
		return info;
	}

	/**
	 * The config SDK context is created in the config-data phase, so its startup logs may be
	 * dropped before the polaris log appenders are ready. Exposing the client id here gives
	 * tooling a reliable source instead of grepping logs.
	 */
	private String getClientId() {
		try {
			return PolarisConfigSDKContextManager.innerGetConfigSDKContext().getValueContext().getClientId();
		}
		catch (Throwable throwable) {
			return null;
		}
	}
}
