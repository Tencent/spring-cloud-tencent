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

package com.tencent.cloud.polaris.config.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.tencent.cloud.common.util.EnvironmentUtils;
import com.tencent.cloud.polaris.config.config.ConfigFileGroup;
import com.tencent.cloud.polaris.config.configdata.PolarisConfigDataLoader;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.CompositePropertySource;

import static com.tencent.cloud.polaris.config.utils.PolarisPropertySourceUtils.loadGroupPolarisPropertySource;
import static com.tencent.cloud.polaris.config.utils.PolarisPropertySourceUtils.loadPolarisPropertySource;

/**
 * PolarisConfigFilePuller pull configFile from Polaris.
 *
 * @author wlx, youta
 */
public final class PolarisConfigFilePuller {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigFileLocator.class);

	private PolarisContextProperties polarisContextProperties;

	private ConfigFileService configFileService;

	private PolarisConfigFilePuller() {
	}

	/**
	 * Factory method to create PolarisConfigFilePuller for
	 * {@link PolarisConfigDataLoader},{@link PolarisConfigFileLocator}.
	 *
	 * @param polarisContextProperties     polarisContextProperties
	 * @param configFileService            configFileService
	 * @return PolarisConfigFilePuller instance
	 */
	public static PolarisConfigFilePuller get(PolarisContextProperties polarisContextProperties, ConfigFileService configFileService) {
		PolarisConfigFilePuller puller = new PolarisConfigFilePuller();
		puller.polarisContextProperties = polarisContextProperties;
		puller.configFileService = configFileService;
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
			PolarisPropertySource polarisPropertySource = loadPolarisPropertySource(configFileService,
					configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName());
			compositePropertySource.addPropertySource(polarisPropertySource);
			PolarisPropertySourceManager.addPropertySource(polarisPropertySource);
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
		String groupNamespace = configFileGroup.getNamespace();
		if (StringUtils.isBlank(groupNamespace)) {
			groupNamespace = polarisContextProperties.getNamespace();
		}
		String group = configFileGroup.getName();
		if (StringUtils.isBlank(group)) {
			throw new IllegalArgumentException("polaris config group name cannot be empty.");
		}
		List<String> files = configFileGroup.getFiles();
		if (CollectionUtils.isEmpty(files)) {
			return;
		}
		for (String fileName : files) {
			PolarisPropertySource polarisPropertySource = loadPolarisPropertySource(configFileService, groupNamespace, group, fileName);
			compositePropertySource.addPropertySource(polarisPropertySource);
			PolarisPropertySourceManager.addPropertySource(polarisPropertySource);
			LOGGER.info(
					"[SCT Config] Load and inject polaris config file success. namespace = {}, group = {}, fileName = {}",
					groupNamespace, group, fileName);
		}
	}

	/**
	 * Init TSF config groups.
	 * @param compositePropertySource compositePropertySource
	 */
	public void initTsfConfigGroups(CompositePropertySource compositePropertySource) {
		String tsfId = System.getProperty("tsf_id");
		String tsfNamespaceName = System.getProperty("tsf_namespace_name");
		String tsfGroupName = System.getProperty("tsf_group_name");

		if (StringUtils.isEmpty(tsfNamespaceName) || StringUtils.isEmpty(tsfGroupName)) {
			return;
		}
		String namespace = polarisContextProperties.getNamespace();
		List<String> tsfConfigGroups = new ArrayList<>();
		tsfConfigGroups.add((StringUtils.isNotBlank(tsfId) ? tsfId + "." : "") + tsfGroupName + ".application_config_group");
		tsfConfigGroups.add((StringUtils.isNotBlank(tsfId) ? tsfId + "." : "") + tsfNamespaceName + ".global_config_group");

		if (EnvironmentUtils.isGateway()) {
			tsfConfigGroups.add((StringUtils.isNotBlank(tsfId) ? tsfId + "." : "") + tsfGroupName + ".gateway_config_group");
		}
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

	private List<ConfigFileMetadata> getInternalConfigFiles(
			String[] activeProfiles, String[] defaultProfiles, String serviceName) {
		String namespace = polarisContextProperties.getNamespace();
		if (StringUtils.isNotBlank(polarisContextProperties.getService())) {
			serviceName = polarisContextProperties.getService();
		}
		// priority: application-${profile} > application > boostrap-${profile} > boostrap
		return getInternalConfigFiles(activeProfiles, defaultProfiles, namespace, serviceName);
	}

	private List<ConfigFileMetadata> getInternalConfigFiles(
			String[] activeProfiles, String[] defaultProfiles, String namespace, String serviceName) {
		List<String> profileList = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(activeProfiles)) {
			profileList.addAll(Arrays.asList(activeProfiles));
		}
		else if (CollectionUtils.isNotEmpty(defaultProfiles)) {
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
			if (StringUtils.isBlank(profile)) {
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
			if (StringUtils.isBlank(profile)) {
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
