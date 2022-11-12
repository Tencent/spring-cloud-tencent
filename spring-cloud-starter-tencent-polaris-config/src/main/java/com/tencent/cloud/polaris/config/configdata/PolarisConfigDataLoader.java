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

package com.tencent.cloud.polaris.config.configdata;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.polaris.config.adapter.PolarisConfigFilePuller;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.config.ConfigFileGroup;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.factory.ConfigFileServiceFactory;
import org.apache.commons.logging.Log;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
	private static final String POLARIS_CONFIG_PROPERTY_SOURCE_NAME = "polaris-config";
	private final Log log;
	private ConfigFileService configFileService;
	private PolarisConfigFilePuller puller;

	public PolarisConfigDataLoader(DeferredLogFactory logFactory) {
		this.log = logFactory.getLog(getClass());
	}

	@Override
	public ConfigData load(ConfigDataLoaderContext context, PolarisConfigDataResource resource)
			throws ConfigDataResourceNotFoundException {
		try {
			return load(context.getBootstrapContext(), resource);
		}
		catch (Exception e) {
			log.warn("Error getting properties from polaris: " + resource, e);
			if (!resource.isOptional()) {
				throw new ConfigDataResourceNotFoundException(resource, e);
			}
			return null;
		}
	}

	public ConfigData load(ConfigurableBootstrapContext bootstrapContext, PolarisConfigDataResource resource) {
		CompositePropertySource compositePropertySource = locate(bootstrapContext, resource);
		return new ConfigData(compositePropertySource.getPropertySources(), getOptions(resource));
	}

	private CompositePropertySource locate(ConfigurableBootstrapContext bootstrapContext,
			PolarisConfigDataResource resource) {
		CompositePropertySource compositePropertySource = new CompositePropertySource(
				POLARIS_CONFIG_PROPERTY_SOURCE_NAME);
		SDKContext sdkContext = bootstrapContext.get(SDKContext.class);
		if (null == this.configFileService) {
			this.configFileService = ConfigFileServiceFactory.createConfigFileService(sdkContext);
		}
		if (null == this.puller) {
			this.puller = PolarisConfigFilePuller.get(resource.getPolarisContextProperties(),
					configFileService, bootstrapContext.get(PolarisPropertySourceManager.class));
		}
		Profiles profiles = resource.getProfiles();
		if (INTERNAL_CONFIG_FILES_LOADED.compareAndSet(false, true)) {
			log.info("loading internal config files");
			String[] activeProfiles = profiles.getActive().toArray(new String[] {});
			String[] defaultProfiles = profiles.getDefault().toArray(new String[] {});
			this.puller.initInternalConfigFiles(
					compositePropertySource, activeProfiles, defaultProfiles, resource.getServiceName());
		}

		PolarisConfigProperties polarisConfigProperties = resource.getPolarisConfigProperties();
		if (!CollectionUtils.isEmpty(polarisConfigProperties.getGroups()) &&
				CUSTOM_POLARIS_CONFIG_FILE_LOADED.compareAndSet(false, true)) {
			log.info("loading custom config files");
			this.puller.initCustomPolarisConfigFiles(compositePropertySource,
					polarisConfigProperties.getGroups());
		}
		// load config data
		if (StringUtils.hasText(resource.getFileName())) {
			log.info("loading config data config file, group:" + resource.getGroupName() + " file: " + resource.getFileName());
			this.puller.initCustomPolarisConfigFile(compositePropertySource, configFileGroup(resource));
		}
		return compositePropertySource;
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
