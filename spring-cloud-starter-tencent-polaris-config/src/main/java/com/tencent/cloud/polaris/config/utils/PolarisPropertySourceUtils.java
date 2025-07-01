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

package com.tencent.cloud.polaris.config.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.polaris.config.adapter.PolarisPropertySource;
import com.tencent.cloud.polaris.config.enums.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.internal.CompositeConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for PolarisPropertySource.
 *
 * @author Haotian Zhang
 */
public final class PolarisPropertySourceUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisPropertySourceUtils.class);

	private PolarisPropertySourceUtils() {

	}

	public static String generateName(String namespace, String group, String fileName) {
		return namespace + "-" + group + "-" + fileName;
	}

	public static PolarisPropertySource loadPolarisPropertySource(ConfigFileService configFileService, String namespace, String group, String fileName) {
		ConfigKVFile configKVFile = loadConfigKVFile(configFileService, namespace, group, fileName);

		Map<String, Object> map = new ConcurrentHashMap<>();
		for (String key : configKVFile.getPropertyNames()) {
			map.put(key, configKVFile.getProperty(key, null));
		}

		return new PolarisPropertySource(namespace, group, fileName, configKVFile, map);
	}

	public static PolarisPropertySource loadGroupPolarisPropertySource(ConfigFileService configFileService, String namespace, String group) {
		List<ConfigKVFile> configKVFiles = new ArrayList<>();

		com.tencent.polaris.configuration.api.core.ConfigFileGroup remoteGroup = configFileService.getConfigFileGroup(namespace, group);
		if (remoteGroup == null) {
			return null;
		}

		for (ConfigFileMetadata configFile : remoteGroup.getConfigFileMetadataList()) {
			String fileName = configFile.getFileName();
			ConfigKVFile configKVFile = loadConfigKVFile(configFileService, namespace, group, fileName);
			configKVFiles.add(configKVFile);
		}

		CompositeConfigFile compositeConfigFile = new CompositeConfigFile(configKVFiles);

		Map<String, Object> map = new ConcurrentHashMap<>();
		for (String key : compositeConfigFile.getPropertyNames()) {
			String value = compositeConfigFile.getProperty(key, null);
			map.put(key, value);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("namespace='" + namespace + '\''
					+ ", group='" + group + '\'' + ", fileName='" + compositeConfigFile + '\''
					+ ", map='" + map + '\'');
		}

		return new PolarisPropertySource(namespace, group, "", compositeConfigFile, map);
	}

	public static ConfigKVFile loadConfigKVFile(ConfigFileService configFileService, String namespace, String group, String fileName) {
		ConfigKVFile configKVFile;
		// unknown extension is resolved as yaml file
		if (ConfigFileFormat.isYamlFile(fileName) || ConfigFileFormat.isUnknownFile(fileName)) {
			configKVFile = configFileService.getConfigYamlFile(namespace, group, fileName);
		}
		else if (ConfigFileFormat.isPropertyFile(fileName)) {
			configKVFile = configFileService.getConfigPropertiesFile(namespace, group, fileName);
		}
		else {
			LOGGER.warn("[SCT Config] Unsupported config file. namespace = {}, group = {}, fileName = {}", namespace, group, fileName);

			throw new IllegalStateException("Only configuration files in the format of properties / yaml / yaml" + " can be injected into the spring context");
		}
		return configKVFile;
	}
}
