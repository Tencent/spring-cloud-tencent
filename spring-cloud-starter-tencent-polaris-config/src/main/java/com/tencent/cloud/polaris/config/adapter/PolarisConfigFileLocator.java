/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 *  Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/BSD-3-Clause
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package com.tencent.cloud.polaris.config.adapter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.polaris.config.config.ConfigFileGroup;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.enums.ConfigFileFormat;
import com.tencent.cloud.polaris.context.PolarisContextProperties;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
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

	private final PolarisPropertySourceManager polarisPropertySourceManager;

	public PolarisConfigFileLocator(PolarisConfigProperties polarisConfigProperties,
			PolarisContextProperties polarisContextProperties, ConfigFileService configFileService,
			PolarisPropertySourceManager polarisPropertySourceManager) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.polarisContextProperties = polarisContextProperties;
		this.configFileService = configFileService;
		this.polarisPropertySourceManager = polarisPropertySourceManager;
	}

	@Override
	public PropertySource<?> locate(Environment environment) {
		CompositePropertySource compositePropertySource = new CompositePropertySource(
				POLARIS_CONFIG_PROPERTY_SOURCE_NAME);

		List<ConfigFileGroup> configFileGroups = polarisConfigProperties.getGroups();
		if (CollectionUtils.isEmpty(configFileGroups)) {
			return compositePropertySource;
		}

		initPolarisConfigFiles(compositePropertySource, configFileGroups);

		return compositePropertySource;
	}

	private void initPolarisConfigFiles(CompositePropertySource compositePropertySource,
			List<ConfigFileGroup> configFileGroups) {
		String namespace = polarisContextProperties.getNamespace();

		for (ConfigFileGroup configFileGroup : configFileGroups) {
			String group = configFileGroup.getName();

			if (StringUtils.isEmpty(group)) {
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
	}

	private PolarisPropertySource loadPolarisPropertySource(String namespace, String group, String fileName) {
		ConfigKVFile configKVFile;
		// unknown extension is resolved as properties file
		if (ConfigFileFormat.isPropertyFile(fileName) || ConfigFileFormat.isUnknownFile(fileName)) {
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

}
