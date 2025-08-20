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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.util.EnvironmentUtils;
import com.tencent.cloud.plugin.tsf.tls.utils.SyncUtils;
import com.tencent.cloud.polaris.config.config.ConfigFileGroup;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfTlsProperties;
import com.tencent.polaris.api.utils.ClassUtils;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import static com.tencent.cloud.polaris.config.utils.PolarisPropertySourceUtils.loadGroupPolarisPropertySource;
import static com.tencent.cloud.polaris.config.utils.PolarisPropertySourceUtils.loadPolarisPropertySource;

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
	private final Environment environment;
	// this class provides customized logic for some customers to configure special business group files
	private final PolarisConfigCustomExtensionLayer polarisConfigCustomExtensionLayer = PolarisServiceLoaderUtil.getPolarisConfigCustomExtensionLayer();

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
				if (CollectionUtils.isNotEmpty(configFileGroups)) {
					initCustomPolarisConfigFiles(compositePropertySource, configFileGroups);
				}
				// load tsf default config group
				initTsfConfigGroups(compositePropertySource);
				// load tsf tls properties if need.
				initTsfTlsPropertySource(compositePropertySource);
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
		if (StringUtils.isBlank(serviceName)) {
			serviceName = environment.getProperty("spring.application.name");
		}

		List<ConfigFileMetadata> internalConfigFiles = new LinkedList<>();

		// priority: application-${profile} > application > boostrap-${profile} > boostrap
		String[] activeProfiles = environment.getActiveProfiles();
		String[] defaultProfiles = environment.getDefaultProfiles();
		List<String> profileList = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(activeProfiles)) {
			profileList.addAll(Arrays.asList(activeProfiles));
		}
		else if (CollectionUtils.isNotEmpty(defaultProfiles)) {
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

	private void buildInternalBootstrapConfigFiles(List<ConfigFileMetadata> internalConfigFiles, String namespace, String serviceName, List<String> profileList) {
		for (String profile : profileList) {
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

	void initTsfConfigGroups(CompositePropertySource compositePropertySource) {
		String tsfId = environment.getProperty("tsf_id");
		String tsfNamespaceName = environment.getProperty("tsf_namespace_name");
		String tsfGroupName = environment.getProperty("tsf_group_name");

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

	void initTsfTlsPropertySource(CompositePropertySource compositePropertySource) {
		String address = System.getProperty("MESH_CITADEL_ADDR", System.getenv("MESH_CITADEL_ADDR"));
		if (StringUtils.isNotBlank(address)
				&& (StringUtils.equals("tsf", environment.getProperty("server.ssl.bundle"))
				|| "tsf".equals(compositePropertySource.getProperty(("server.ssl.bundle"))))
				&& ClassUtils.isClassPresent("com.tencent.cloud.plugin.tsf.tls.utils.SyncUtils")
				&& !SyncUtils.isInitialized()) {
			// get common name
			Object commonName = compositePropertySource.getProperty("spring.cloud.polaris.service");
			if (commonName == null) {
				commonName = compositePropertySource.getProperty("spring.cloud.polaris.discovery.service");
			}
			if (commonName == null) {
				commonName = compositePropertySource.getProperty("spring.application.name");
			}
			if (commonName == null) {
				commonName = environment.getProperty("spring.cloud.polaris.service");
			}
			if (commonName == null) {
				commonName = environment.getProperty("spring.cloud.polaris.discovery.service");
			}
			if (commonName == null) {
				commonName = environment.getProperty("spring.application.name");
			}
			// get certPath
			String certPath = System.getProperty("MESH_CITADEL_CERT", System.getenv("MESH_CITADEL_CERT"));
			// get token
			String token = System.getProperty("tsf_token", System.getenv("tsf_token"));
			// get validityDuration
			Object validityDuration = compositePropertySource.getProperty("spring.cloud.polaris.tls.validityDuration");
			if (validityDuration == null) {
				validityDuration = environment.getProperty("spring.cloud.polaris.tls.validityDuration", Long.class, TsfTlsProperties.DEFAULT_VALIDITY_DURATION);
			}
			if (validityDuration instanceof String) {
				validityDuration = Long.valueOf((String) validityDuration);
			}
			// get refreshBefore
			Object refreshBefore = compositePropertySource.getProperty("spring.cloud.polaris.tls.refreshBefore");
			if (refreshBefore == null) {
				refreshBefore = environment.getProperty("spring.cloud.polaris.tls.refreshBefore", Long.class, TsfTlsProperties.DEFAULT_REFRESH_BEFORE);
			}
			if (refreshBefore instanceof String) {
				refreshBefore = Long.valueOf((String) refreshBefore);
			}
			// get watchInterval
			Object watchInterval = compositePropertySource.getProperty("spring.cloud.polaris.tls.watchInterval");
			if (watchInterval == null) {
				watchInterval = environment.getProperty("spring.cloud.polaris.tls.watchInterval", Long.class, TsfTlsProperties.DEFAULT_WATCH_INTERVAL);
			}
			if (watchInterval instanceof String) {
				watchInterval = Long.valueOf((String) watchInterval);
			}
			SyncUtils.init((String) commonName, address, certPath, token, (Long) validityDuration, (Long) refreshBefore, (Long) watchInterval);
			if (SyncUtils.isVerified()) {
				Map<String, Object> tlsEnvProperties = new HashMap<>();
				// set ssl
				Object clientAuth = compositePropertySource.getProperty("server.ssl.client-auth");
				if (clientAuth == null) {
					clientAuth = environment.getProperty("server.ssl.client-auth", "want");
				}
				tlsEnvProperties.put("server.ssl.client-auth", clientAuth);
				Object protocol = compositePropertySource.getProperty("spring.cloud.polaris.discovery.protocol");
				if (protocol == null) {
					protocol = environment.getProperty("spring.cloud.polaris.discovery.protocol", "https");
				}
				tlsEnvProperties.put("spring.cloud.polaris.discovery.protocol", protocol);
				tlsEnvProperties.put("tsf.discovery.scheme", protocol);
				// set tsf spring ssl bundle
				tlsEnvProperties.put("spring.ssl.bundle.pem.tsf.reload-on-update", "true");
				if (StringUtils.isNotBlank(SyncUtils.getPemKeyStoreCertPath()) && StringUtils.isNotBlank(SyncUtils.getPemKeyStoreKeyPath())) {
					tlsEnvProperties.put("spring.ssl.bundle.pem.tsf.keystore.certificate", SyncUtils.getPemKeyStoreCertPath());
					tlsEnvProperties.put("spring.ssl.bundle.pem.tsf.keystore.private-key", SyncUtils.getPemKeyStoreKeyPath());
				}
				if (StringUtils.isNotBlank(SyncUtils.getPemTrustStoreCertPath())) {
					tlsEnvProperties.put("spring.ssl.bundle.pem.tsf.truststore.certificate", SyncUtils.getPemTrustStoreCertPath());
				}

				// process environment
				MapPropertySource propertySource = new MapPropertySource("tsf-tls-config", tlsEnvProperties);
				compositePropertySource.addPropertySource(propertySource);
			}
		}
	}

	private void initCustomPolarisConfigFiles(CompositePropertySource compositePropertySource, List<ConfigFileGroup> configFileGroups) {
		String namespace = polarisContextProperties.getNamespace();

		for (ConfigFileGroup configFileGroup : configFileGroups) {
			String groupNamespace = configFileGroup.getNamespace();
			if (StringUtils.isBlank(groupNamespace)) {
				groupNamespace = namespace;
			}

			String group = configFileGroup.getName();
			if (StringUtils.isBlank(group)) {
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
}
