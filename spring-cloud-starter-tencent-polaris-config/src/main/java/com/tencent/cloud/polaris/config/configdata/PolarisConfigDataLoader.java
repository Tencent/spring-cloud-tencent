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

package com.tencent.cloud.polaris.config.configdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.polaris.config.PolarisConfigSDKContextManager;
import com.tencent.cloud.polaris.config.adapter.PolarisConfigCustomExtensionLayer;
import com.tencent.cloud.polaris.config.adapter.PolarisConfigFilePuller;
import com.tencent.cloud.polaris.config.adapter.PolarisServiceLoaderUtil;
import com.tencent.cloud.polaris.config.config.ConfigFileGroup;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.factory.ConfigFileServiceFactory;
import org.apache.commons.logging.Log;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;

import static org.springframework.boot.context.config.ConfigData.Option.IGNORE_IMPORTS;
import static org.springframework.boot.context.config.ConfigData.Option.IGNORE_PROFILES;
import static org.springframework.boot.context.config.ConfigData.Option.PROFILE_SPECIFIC;

/**
 * Implementation of {@link ConfigDataLoader}.can be used to load {@link ConfigData} for a given
 * {@link PolarisConfigDataResource} .
 * <p>
 * Load {@link ConfigData} via {@link PolarisConfigDataLoader}
 *
 * @author wlx
 */
public class PolarisConfigDataLoader implements ConfigDataLoader<PolarisConfigDataResource> {

	static final AtomicBoolean INTERNAL_CONFIG_FILES_LOADED = new AtomicBoolean(false);
	static final AtomicBoolean CUSTOM_POLARIS_CONFIG_FILE_LOADED = new AtomicBoolean(false);
	static final AtomicBoolean TSF_CONFIG_FILE_LOADED = new AtomicBoolean(false);
	static final AtomicBoolean TSF_TLS_CONFIG_FILE_LOADED = new AtomicBoolean(false);
	private static final String POLARIS_CONFIG_PROPERTY_SOURCE_NAME = "polaris-config";
	private final Log log;
	private final DeferredLogFactory logFactory;
	private final PolarisConfigCustomExtensionLayer polarisConfigCustomExtensionLayer = PolarisServiceLoaderUtil.getPolarisConfigCustomExtensionLayer();
	private ConfigFileService configFileService;
	private PolarisConfigFilePuller puller;

	public PolarisConfigDataLoader(DeferredLogFactory logFactory) {
		this.logFactory = logFactory;
		this.log = logFactory.getLog(getClass());
	}

	@Override
	public ConfigData load(ConfigDataLoaderContext context, PolarisConfigDataResource resource)
			throws ConfigDataResourceNotFoundException {
		try {
			return load(resource);
		}
		catch (Exception e) {
			log.warn("Error getting properties from polaris: " + resource, e);
			if (!resource.isOptional()) {
				throw new ConfigDataResourceNotFoundException(resource, e);
			}
			return null;
		}
	}

	public ConfigData load(PolarisConfigDataResource resource) {
		CompositePropertySource compositePropertySource = locate(resource);
		List<PropertySource<?>> propertySourceList = new ArrayList<>(compositePropertySource.getPropertySources());
		Collections.reverse(propertySourceList);
		return new ConfigData(propertySourceList, getOptions(resource));
	}

	private CompositePropertySource locate(PolarisConfigDataResource resource) {
		CompositePropertySource compositePropertySource = new CompositePropertySource(
				POLARIS_CONFIG_PROPERTY_SOURCE_NAME);
		SDKContext sdkContext = PolarisConfigSDKContextManager.innerGetConfigSDKContext();
		if (null == this.configFileService) {
			this.configFileService = ConfigFileServiceFactory.createConfigFileService(sdkContext);
		}
		if (null == this.puller) {
			this.puller = PolarisConfigFilePuller.get(resource.getPolarisContextProperties(), configFileService, logFactory);
		}
		PolarisConfigProperties polarisConfigProperties = resource.getPolarisConfigProperties();
		try {
			// load custom config extension files
			initCustomPolarisConfigExtensionFiles(compositePropertySource);
			// load spring boot default config files
			initInternalConfigFiles(compositePropertySource, polarisConfigProperties, resource);
			// load custom config files
			initCustomPolarisConfigFiles(compositePropertySource, polarisConfigProperties);
			// load config data
			initCustomPolarisConfigDataFiles(compositePropertySource, resource);
			// load tsf default config group
			initTsfConfigGroups(compositePropertySource);
			// load tsf tls properties if need.
			initTsfTlsPropertySource(compositePropertySource, resource);
		}
		finally {
			afterLocatePolarisConfigExtension(compositePropertySource);
		}

		return compositePropertySource;
	}

	private void initCustomPolarisConfigExtensionFiles(CompositePropertySource compositePropertySource) {
		if (polarisConfigCustomExtensionLayer == null) {
			log.debug("[SCT Config] PolarisConfigCustomExtensionLayer is not init, ignore the following execution steps");
			return;
		}
		polarisConfigCustomExtensionLayer.initConfigFiles(null, compositePropertySource, configFileService);
	}

	private void afterLocatePolarisConfigExtension(CompositePropertySource compositePropertySource) {
		if (polarisConfigCustomExtensionLayer == null) {
			log.debug("[SCT Config] PolarisConfigCustomExtensionLayer is not init, ignore the following execution steps");
			return;
		}
		polarisConfigCustomExtensionLayer.executeAfterLocateConfigReturning(compositePropertySource);
	}

	private void initInternalConfigFiles(CompositePropertySource compositePropertySource,
			PolarisConfigProperties polarisConfigProperties, PolarisConfigDataResource resource) {
		Profiles profiles = resource.getProfiles();
		if (polarisConfigProperties.isInternalEnabled() && INTERNAL_CONFIG_FILES_LOADED.compareAndSet(false, true)) {
			String[] activeProfiles = profiles.getActive().toArray(new String[] {});
			String[] defaultProfiles = profiles.getDefault().toArray(new String[] {});
			this.puller.initInternalConfigFiles(
					compositePropertySource, activeProfiles, defaultProfiles, resource.getServiceName());
		}
	}

	private void initCustomPolarisConfigFiles(CompositePropertySource compositePropertySource,
			PolarisConfigProperties polarisConfigProperties) {
		if (!CollectionUtils.isEmpty(polarisConfigProperties.getGroups()) &&
				CUSTOM_POLARIS_CONFIG_FILE_LOADED.compareAndSet(false, true)) {
			this.puller.initCustomPolarisConfigFiles(compositePropertySource,
					polarisConfigProperties.getGroups());
		}
	}

	private void initCustomPolarisConfigDataFiles(CompositePropertySource compositePropertySource,
			PolarisConfigDataResource resource) {
		if (StringUtils.isNotBlank(resource.getFileName())) {
			log.info("[SCT Config] Loading polaris custom config data file, group:" + resource.getGroupName() + " file: " + resource.getFileName());
			this.puller.initCustomPolarisConfigFile(compositePropertySource, configFileGroup(resource));
		}
	}

	private void initTsfConfigGroups(CompositePropertySource compositePropertySource) {
		if (TSF_CONFIG_FILE_LOADED.compareAndSet(false, true)) {
			this.puller.initTsfConfigGroups(compositePropertySource);
		}
	}

	private void initTsfTlsPropertySource(CompositePropertySource compositePropertySource, PolarisConfigDataResource resource) {
		if (TSF_TLS_CONFIG_FILE_LOADED.compareAndSet(false, true)) {
			this.puller.initTsfTlsPropertySource(compositePropertySource, resource.getTsfTlsProperties(), null, resource.getServiceName());
		}
	}

	private ConfigData.Option[] getOptions(PolarisConfigDataResource resource) {
		List<ConfigData.Option> options = new ArrayList<>();
		options.add(IGNORE_IMPORTS);
		options.add(IGNORE_PROFILES);
		PolarisConfigProperties polarisConfigProperties = resource.getPolarisConfigProperties();
		if (polarisConfigProperties.isPreference()) {
			// mark it as 'PROFILE_SPECIFIC' config, it has higher priority
			options.add(PROFILE_SPECIFIC);
		}
		return options.toArray(new ConfigData.Option[] {});
	}

	private ConfigFileGroup configFileGroup(PolarisConfigDataResource polarisConfigDataResource) {
		String fileName = polarisConfigDataResource.getFileName();
		String groupName = polarisConfigDataResource.getGroupName();
		ConfigFileGroup configFileGroup = new ConfigFileGroup();
		configFileGroup.setName(groupName);
		List<String> files = new ArrayList<>();
		files.add(fileName);
		configFileGroup.setFiles(files);
		return configFileGroup;
	}
}
