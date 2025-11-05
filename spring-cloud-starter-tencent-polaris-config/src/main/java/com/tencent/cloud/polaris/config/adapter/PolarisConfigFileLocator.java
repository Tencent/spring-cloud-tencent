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

import java.util.List;

import com.tencent.cloud.polaris.config.config.ConfigFileGroup;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

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
	private volatile static CompositePropertySource compositePropertySourceCache = null;
	private final PolarisConfigProperties polarisConfigProperties;
	private final PolarisContextProperties polarisContextProperties;
	private final ConfigFileService configFileService;
	private final PolarisConfigFilePuller puller;
	private final Environment environment;
	// this class provides customized logic for some customers to configure special business group files
	private final PolarisConfigCustomExtensionLayer polarisConfigCustomExtensionLayer = PolarisServiceLoaderUtil.getPolarisConfigCustomExtensionLayer();

	public PolarisConfigFileLocator(PolarisConfigProperties polarisConfigProperties,
			PolarisContextProperties polarisContextProperties, ConfigFileService configFileService, Environment environment) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.polarisContextProperties = polarisContextProperties;
		this.configFileService = configFileService;
		this.puller = PolarisConfigFilePuller.get(polarisContextProperties, configFileService);
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
				initCustomPolarisConfigFiles(compositePropertySource);
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
		// priority: application-${profile} > application > boostrap-${profile} > boostrap
		String[] activeProfiles = environment.getActiveProfiles();
		String[] defaultProfiles = environment.getDefaultProfiles();
		String serviceName = polarisContextProperties.getService();
		if (StringUtils.isBlank(serviceName)) {
			serviceName = environment.getProperty("spring.application.name");
		}
		this.puller.initInternalConfigFiles(compositePropertySource, activeProfiles, defaultProfiles, serviceName);
	}

	private void initCustomPolarisConfigFiles(CompositePropertySource compositePropertySource) {
		List<ConfigFileGroup> configFileGroups = polarisConfigProperties.getGroups();
		if (CollectionUtils.isNotEmpty(configFileGroups)) {
			this.puller.initCustomPolarisConfigFiles(compositePropertySource, configFileGroups);
		}
	}

	private void initTsfConfigGroups(CompositePropertySource compositePropertySource) {
		this.puller.initTsfConfigGroups(compositePropertySource);
	}
}
