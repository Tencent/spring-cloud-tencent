package com.tencent.cloud.polaris.config.adapter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.polaris.config.config.ConfigFileGroup;
import com.tencent.cloud.polaris.config.enums.ConfigFileFormat;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


/**
 * Pull configFile from Polaris
 *
 * @author wlx
 */
public class PolarisConfigFilePuller {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigFileLocator.class);

	private final PolarisContextProperties polarisContextProperties;

	private final ConfigFileService configFileService;

	private final PolarisPropertySourceManager polarisPropertySourceManager;

	private Environment environment;

	public PolarisConfigFilePuller(PolarisContextProperties polarisContextProperties,
								   ConfigFileService configFileService,
								   PolarisPropertySourceManager polarisPropertySourceManager,
								   Environment environment) {
		this.polarisContextProperties = polarisContextProperties;
		this.configFileService = configFileService;
		this.polarisPropertySourceManager = polarisPropertySourceManager;
		this.environment = environment;
	}

	public PolarisConfigFilePuller(PolarisContextProperties polarisContextProperties,
								   ConfigFileService configFileService,
								   PolarisPropertySourceManager polarisPropertySourceManager) {
		this.polarisContextProperties = polarisContextProperties;
		this.configFileService = configFileService;
		this.polarisPropertySourceManager = polarisPropertySourceManager;
	}

	public void initInternalConfigFiles(CompositePropertySource compositePropertySource) {
		List<ConfigFileMetadata> internalConfigFiles = getInternalConfigFiles();

		for (ConfigFileMetadata configFile : internalConfigFiles) {
			PolarisPropertySource polarisPropertySource = loadPolarisPropertySource(
					configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName());

			compositePropertySource.addPropertySource(polarisPropertySource);

			polarisPropertySourceManager.addPropertySource(polarisPropertySource);

			LOGGER.info("[SCT Config] Load and inject polaris config file. file = {}", configFile);
		}
	}

	public List<ConfigFileMetadata> getInternalConfigFiles() {
		String namespace = polarisContextProperties.getNamespace();
		String serviceName = polarisContextProperties.getService();
		if (!StringUtils.hasText(serviceName)) {
			serviceName = environment.getProperty("spring.application.name");
		}

		List<ConfigFileMetadata> internalConfigFiles = new LinkedList<>();

		// priority: application-${profile} > application > boostrap-${profile} > boostrap
		String[] activeProfiles = environment.getActiveProfiles();

		for (String activeProfile : activeProfiles) {
			if (!StringUtils.hasText(activeProfile)) {
				continue;
			}

			internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "application-" + activeProfile + ".properties"));
			internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "application-" + activeProfile + ".yml"));
		}

		internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "application.properties"));
		internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "application.yml"));

		for (String activeProfile : activeProfiles) {
			if (!StringUtils.hasText(activeProfile)) {
				continue;
			}

			internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "bootstrap-" + activeProfile + ".properties"));
			internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "bootstrap-" + activeProfile + ".yml"));
		}

		internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "bootstrap.properties"));
		internalConfigFiles.add(new DefaultConfigFileMetadata(namespace, serviceName, "bootstrap.yml"));


		return internalConfigFiles;
	}


	public void initCustomPolarisConfigFiles(CompositePropertySource compositePropertySource,
											 List<ConfigFileGroup> configFileGroups) {
		configFileGroups.forEach(
				configFileGroup -> initCustomPolarisConfigFile(compositePropertySource, configFileGroup)
		);
	}

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
		} else if (ConfigFileFormat.isYamlFile(fileName)) {
			configKVFile = configFileService.getConfigYamlFile(namespace, group, fileName);
		} else {
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
