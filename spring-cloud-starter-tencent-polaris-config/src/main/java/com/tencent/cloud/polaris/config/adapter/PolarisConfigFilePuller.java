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

package com.tencent.cloud.polaris.config.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.polaris.config.config.ConfigFileGroup;
import com.tencent.cloud.polaris.config.configdata.PolarisConfigDataLoader;
import com.tencent.cloud.polaris.config.enums.ConfigFileFormat;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * PolarisConfigFilePuller pull configFile from Polaris.
 *
 * @author wlx, youta
 */
public final class PolarisConfigFilePuller {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigFileLocator.class);

	private PolarisContextProperties polarisContextProperties;

	private ConfigFileService configFileService;

	private PolarisPropertySourceManager polarisPropertySourceManager;

	private PolarisConfigFilePuller() {
	}

	/**
	 * Factory method to create PolarisConfigFilePuller for
	 * {@link PolarisConfigDataLoader},{@link PolarisConfigFileLocator}.
	 *
	 * @param polarisContextProperties     polarisContextProperties
	 * @param configFileService            configFileService
	 * @param polarisPropertySourceManager polarisPropertySourceManager
	 * @return PolarisConfigFilePuller instance
	 */
	public static PolarisConfigFilePuller get(PolarisContextProperties polarisContextProperties, ConfigFileService configFileService,
			PolarisPropertySourceManager polarisPropertySourceManager) {
		PolarisConfigFilePuller puller = new PolarisConfigFilePuller();
		puller.polarisContextProperties = polarisContextProperties;
		puller.configFileService = configFileService;
		puller.polarisPropertySourceManager = polarisPropertySourceManager;
		return puller;
	}

	/**
	 * InitInternalConfigFiles for {@link PolarisConfigDataLoader}.
	 *
	 * @param compositePropertySource compositePropertySource
	 * @param activeProfiles          activeProfiles
	 * @param defaultProfiles         defaultProfiles
	 * @param serviceName             serviceName
	 */
	public void initInternalConfigFiles(CompositePropertySource compositePropertySource, String[] activeProfiles,
			String[] defaultProfiles, String serviceName) {
		List<ConfigFileMetadata> internalConfigFiles = getInternalConfigFiles(activeProfiles, defaultProfiles, serviceName);
		for (ConfigFileMetadata configFile : internalConfigFiles) {
			PolarisPropertySource polarisPropertySource = loadPolarisPropertySource(
					configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName());
			compositePropertySource.addPropertySource(polarisPropertySource);
			polarisPropertySourceManager.addPropertySource(polarisPropertySource);
			LOGGER.info("[SCT Config] Load and inject polaris config file. file = {}", configFile);
		}
	}

	/**
	 * Init multiple CustomPolarisConfigFile.
	 *
	 * @param compositePropertySource compositePropertySource
	 * @param configFileGroups        configFileGroups
	 */
	public void initCustomPolarisConfigFiles(CompositePropertySource compositePropertySource,
			List<ConfigFileGroup> configFileGroups) {
		configFileGroups.forEach(
				configFileGroup -> initCustomPolarisConfigFile(compositePropertySource, configFileGroup)
		);
	}

	/**
	 * Init single CustomPolarisConfigFile.
	 *
	 * @param compositePropertySource compositePropertySource
	 * @param configFileGroup         configFileGroup
	 */
	public void initCustomPolarisConfigFile(CompositePropertySource compositePropertySource,
			ConfigFileGroup configFileGroup) {
		String namespace = polarisContextProperties.getNamespace();
		String group = configFileGroup.getName();
		if (!StringUtils.hasText(group)) {
			throw new IllegalArgumentException("polaris config group name cannot be empty.");
		}
		List<String> files = configFileGroup.getFiles();
		if (CollectionUtils.isEmpty(files)) {
			return;
		}
		for (String fileName : files) {
			PolarisPropertySource polarisPropertySource = loadPolarisPropertySource(namespace, group, fileName);
			compositePropertySource.addPropertySource(polarisPropertySource);
			polarisPropertySourceManager.addPropertySource(polarisPropertySource);
			LOGGER.info(
					"[SCT Config] Load and inject polaris config file success. namespace = {}, group = {}, fileName = {}",
					namespace, group, fileName);
		}
	}

	private PolarisPropertySource loadPolarisPropertySource(String namespace, String group, String fileName) {
		ConfigKVFile configKVFile;
		// unknown extension is resolved as properties file
		if (ConfigFileFormat.isPropertyFile(fileName)
				|| ConfigFileFormat.isUnknownFile(fileName)) {
			configKVFile = configFileService.getConfigPropertiesFile(namespace, group, fileName);
		}
		else if (ConfigFileFormat.isYamlFile(fileName)) {
			configKVFile = configFileService.getConfigYamlFile(namespace, group, fileName);
		}
		else {
			LOGGER.warn("[SCT Config] Unsupported config file. namespace = {}, group = {}, fileName = {}", namespace,
					group, fileName);

			throw new IllegalStateException("Only configuration files in the format of properties / yaml / yaml"
					+ " can be injected into the spring context");
		}
		Map<String, Object> map = new ConcurrentHashMap<>();
		for (String key : configKVFile.getPropertyNames()) {
			map.put(key, configKVFile.getProperty(key, null));
		}
		return new PolarisPropertySource(namespace, group, fileName, configKVFile, map);
	}

	private List<ConfigFileMetadata> getInternalConfigFiles(
			String[] activeProfiles, String[] defaultProfiles, String serviceName) {
		String namespace = polarisContextProperties.getNamespace();
		if (StringUtils.hasText(polarisContextProperties.getService())) {
			serviceName = polarisContextProperties.getService();
		}
		// priority: application-${profile} > application > boostrap-${profile} > boostrap
		return getInternalConfigFiles(activeProfiles, defaultProfiles, namespace, serviceName);
	}

	private List<ConfigFileMetadata> getInternalConfigFiles(
			String[] activeProfiles, String[] defaultProfiles, String namespace, String serviceName) {
		List<String> profileList = new ArrayList<>();
		if (ArrayUtils.isNotEmpty(activeProfiles)) {
			profileList.addAll(Arrays.asList(activeProfiles));
		}
		else if (ArrayUtils.isNotEmpty(defaultProfiles)) {
			profileList.addAll(Arrays.asList(defaultProfiles));
		}

		List<ConfigFileMetadata> internalConfigFiles = new LinkedList<>();
		// build application config files
		buildInternalApplicationConfigFiles(internalConfigFiles, namespace, serviceName, profileList);
		// build bootstrap config files
		buildInternalBootstrapConfigFiles(internalConfigFiles, namespace, serviceName, profileList);

		return internalConfigFiles;
	}

	private void buildInternalApplicationConfigFiles(
			List<ConfigFileMetadata> internalConfigFiles, String namespace, String serviceName, List<String> profiles) {
		for (String profile : profiles) {
			if (!StringUtils.hasText(profile)) {
				continue;
			}
			internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "application-" + profile + ".properties"));
			internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "application-" + profile + ".yml"));
			internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "application-" + profile + ".yaml"));
		}
		// build default config properties files.
		internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "application.properties"));
		internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "application.yml"));
		internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "application.yaml"));
	}

	private void buildInternalBootstrapConfigFiles(
			List<ConfigFileMetadata> internalConfigFiles, String namespace, String serviceName, List<String> profiles) {
		for (String profile : profiles) {
			if (!StringUtils.hasText(profile)) {
				continue;
			}
			internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "bootstrap-" + profile + ".properties"));
			internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "bootstrap-" + profile + ".yml"));
			internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "bootstrap-" + profile + ".yaml"));
		}
		// build default config properties files.
		internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "bootstrap.properties"));
		internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "bootstrap.yml"));
		internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "bootstrap.yaml"));
	}
}
