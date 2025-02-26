/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.enums.ConfigFileFormat;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.internal.CompositeConfigFile;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Spring cloud reserved core configuration loading SPI.
 * <p>
 * This SPI is implemented to interface with Polaris configuration center
 *
 * @author lepdou 2022-03-10
 */
@Order(0)
public class PolarisConfigFileLocator implements PropertySourceLocator {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigFileLocator.class);

	private static final String POLARIS_CONFIG_PROPERTY_SOURCE_NAME = "polaris-config";

	private final PolarisConfigProperties polarisConfigProperties;

	private final PolarisContextProperties polarisContextProperties;

	private final ConfigFileService configFileService;

	private final Environment environment;
	// this class provides customized logic for some customers to configure special business group files
	private final PolarisConfigCustomExtensionLayer polarisConfigCustomExtensionLayer = PolarisServiceLoaderUtil.getPolarisConfigCustomExtensionLayer();

	private volatile static CompositePropertySource compositePropertySourceCache = null;

	public PolarisConfigFileLocator(PolarisConfigProperties polarisConfigProperties,
			PolarisContextProperties polarisContextProperties, ConfigFileService configFileService, Environment environment) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.polarisContextProperties = polarisContextProperties;
		this.configFileService = configFileService;
		this.environment = environment;
	}

	/**
	 *  order: spring boot default config files > custom config files > tsf default config group.
	 *  @param environment The current Environment.
	 *  @return The PropertySource to be added to the Environment.
	 */
	@Override
	public PropertySource<?> locate(Environment environment) {
		if (polarisConfigProperties.isEnabled()) {
			// use cache when refreshing context
			if (compositePropertySourceCache != null) {
				return compositePropertySourceCache;
			}

			CompositePropertySource compositePropertySource = new CompositePropertySource(POLARIS_CONFIG_PROPERTY_SOURCE_NAME);
			compositePropertySourceCache = compositePropertySource;
			try {
				// load custom config extension files
				initCustomPolarisConfigExtensionFiles(compositePropertySource);
				// load spring boot default config files
				initInternalConfigFiles(compositePropertySource);
				// load custom config files
				List<ConfigFileGroup> configFileGroups = polarisConfigProperties.getGroups();
				if (!CollectionUtils.isEmpty(configFileGroups)) {
					initCustomPolarisConfigFiles(compositePropertySource, configFileGroups);
				}
				// load tsf default config group
				initTsfConfigGroups(compositePropertySource);
				return compositePropertySource;
			}
			finally {
				afterLocatePolarisConfigExtension(compositePropertySource);
			}
		}
		return null;
	}

	private void initCustomPolarisConfigExtensionFiles(CompositePropertySource compositePropertySource) {
		if (polarisConfigCustomExtensionLayer == null) {
			LOGGER.debug("[SCT Config] PolarisConfigCustomExtensionLayer is not init, ignore the following execution steps");
			return;
		}
		polarisConfigCustomExtensionLayer.initConfigFiles(environment, compositePropertySource, configFileService);
	}

	private void afterLocatePolarisConfigExtension(CompositePropertySource compositePropertySource) {
		if (polarisConfigCustomExtensionLayer == null) {
			LOGGER.debug("[SCT Config] PolarisConfigCustomExtensionLayer is not init, ignore the following execution steps");
			return;
		}
		polarisConfigCustomExtensionLayer.executeAfterLocateConfigReturning(compositePropertySource);
	}

	private void initInternalConfigFiles(CompositePropertySource compositePropertySource) {
		if (!polarisConfigProperties.isInternalEnabled()) {
			return;
		}
		List<ConfigFileMetadata> internalConfigFiles = getInternalConfigFiles();

		for (ConfigFileMetadata configFile : internalConfigFiles) {
			if (StringUtils.isEmpty(configFile.getFileGroup())) {
				continue;
			}
			PolarisPropertySource polarisPropertySource = loadPolarisPropertySource(configFileService, configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName());

			compositePropertySource.addPropertySource(polarisPropertySource);

			PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

			LOGGER.info("[SCT Config] Load and inject polaris config file. file = {}", configFile);
		}
	}

	private List<ConfigFileMetadata> getInternalConfigFiles() {
		String namespace = polarisContextProperties.getNamespace();
		String serviceName = polarisContextProperties.getService();
		if (!StringUtils.hasText(serviceName)) {
			serviceName = environment.getProperty("spring.application.name");
		}

		List<ConfigFileMetadata> internalConfigFiles = new LinkedList<>();

		// priority: application-${profile} > application > boostrap-${profile} > boostrap
		String[] activeProfiles = environment.getActiveProfiles();
		String[] defaultProfiles = environment.getDefaultProfiles();
		List<String> profileList = new ArrayList<>();
		if (ArrayUtils.isNotEmpty(activeProfiles)) {
			profileList.addAll(Arrays.asList(activeProfiles));
		}
		else if (ArrayUtils.isNotEmpty(defaultProfiles)) {
			profileList.addAll(Arrays.asList(defaultProfiles));
		}
		// build application config files
		buildInternalApplicationConfigFiles(internalConfigFiles, namespace, serviceName, profileList);
		// build bootstrap config files
		buildInternalBootstrapConfigFiles(internalConfigFiles, namespace, serviceName, profileList);

		return internalConfigFiles;
	}

	private void buildInternalApplicationConfigFiles(List<ConfigFileMetadata> internalConfigFiles, String namespace, String serviceName, List<String> profileList) {
		for (String profile : profileList) {
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

	private void buildInternalBootstrapConfigFiles(List<ConfigFileMetadata> internalConfigFiles, String namespace, String serviceName, List<String> profileList) {
		for (String profile : profileList) {
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

	void initTsfConfigGroups(CompositePropertySource compositePropertySource) {
		String tsfId = environment.getProperty("tsf_id");
		String tsfNamespaceName = environment.getProperty("tsf_namespace_name");
		String tsfGroupName = environment.getProperty("tsf_group_name");

		if (StringUtils.isEmpty(tsfId) || StringUtils.isEmpty(tsfNamespaceName) || StringUtils.isEmpty(tsfGroupName)) {
			return;
		}
		String namespace = polarisContextProperties.getNamespace();
		List<String> tsfConfigGroups = Arrays.asList(
				tsfId + "." + tsfGroupName + ".application_config_group",
				tsfId + "." + tsfNamespaceName + ".global_config_group");
		for (String tsfConfigGroup : tsfConfigGroups) {
			PolarisPropertySource polarisPropertySource = loadGroupPolarisPropertySource(configFileService, namespace, tsfConfigGroup);
			if (polarisPropertySource == null) {
				// not register to polaris
				continue;
			}
			compositePropertySource.addPropertySource(polarisPropertySource);
			PolarisPropertySourceManager.addPropertySource(polarisPropertySource);
		}
	}

	private void initCustomPolarisConfigFiles(CompositePropertySource compositePropertySource, List<ConfigFileGroup> configFileGroups) {
		String namespace = polarisContextProperties.getNamespace();

		for (ConfigFileGroup configFileGroup : configFileGroups) {
			String groupNamespace = configFileGroup.getNamespace();
			if (!StringUtils.hasText(groupNamespace)) {
				groupNamespace = namespace;
			}

			String group = configFileGroup.getName();
			if (!StringUtils.hasText(group)) {
				continue;
			}

			List<String> files = configFileGroup.getFiles();

			if (CollectionUtils.isEmpty(files)) {
				PolarisPropertySource polarisPropertySource = loadGroupPolarisPropertySource(configFileService, namespace, group);
				if (polarisPropertySource == null) {
					continue;
				}
				compositePropertySource.addPropertySource(polarisPropertySource);
				PolarisPropertySourceManager.addPropertySource(polarisPropertySource);
				LOGGER.info("[SCT Config] Load and inject polaris config file success. namespace = {}, group = {}", namespace, group);
			}
			else {
				for (String fileName : files) {
					PolarisPropertySource polarisPropertySource = loadPolarisPropertySource(configFileService, groupNamespace, group, fileName);

					compositePropertySource.addPropertySource(polarisPropertySource);

					PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

					LOGGER.info("[SCT Config] Load and inject polaris config file success. namespace = {}, group = {}, fileName = {}", groupNamespace, group, fileName);
				}
			}
		}
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
		// unknown extension is resolved as properties file
		if (ConfigFileFormat.isPropertyFile(fileName) || ConfigFileFormat.isUnknownFile(fileName)) {
			configKVFile = configFileService.getConfigPropertiesFile(namespace, group, fileName);
		}
		else if (ConfigFileFormat.isYamlFile(fileName)) {
			configKVFile = configFileService.getConfigYamlFile(namespace, group, fileName);
		}
		else {
			LOGGER.warn("[SCT Config] Unsupported config file. namespace = {}, group = {}, fileName = {}", namespace, group, fileName);

			throw new IllegalStateException("Only configuration files in the format of properties / yaml / yaml" + " can be injected into the spring context");
		}
		return configKVFile;
	}
}
