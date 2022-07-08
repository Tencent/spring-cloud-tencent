package com.tencent.cloud.polaris.config.configdata;

import com.tencent.cloud.polaris.config.adapter.PolarisPropertySource;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.config.ConfigFileGroup;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.enums.ConfigFileFormat;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import com.tencent.polaris.configuration.factory.ConfigFileServiceFactory;
import org.apache.commons.logging.Log;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.boot.context.config.ConfigData.Option.IGNORE_IMPORTS;
import static org.springframework.boot.context.config.ConfigData.Option.IGNORE_PROFILES;
import static org.springframework.boot.context.config.ConfigData.Option.PROFILE_SPECIFIC;

/**
 * Implementation of {@link ConfigDataLoader}.can be used to load {@link ConfigData} for a given
 * {@link PolarisConfigDataResource}
 * <p>
 * Load {@link ConfigData} via {@link PolarisConfigDataLoader}
 *
 * @author wlx
 * @date 2022/7/5 11:14 下午
 */
public class PolarisConfigDataLoader implements ConfigDataLoader<PolarisConfigDataResource> {

	private static final String POLARIS_CONFIG_PROPERTY_SOURCE_NAME = "polaris-config";

	private final Log log;

	public PolarisConfigDataLoader(DeferredLogFactory logFactory) {
		this.log = logFactory.getLog(getClass());
	}

	@Override
	public ConfigData load(ConfigDataLoaderContext context, PolarisConfigDataResource resource)
			throws IOException, ConfigDataResourceNotFoundException {
		ConfigurableBootstrapContext bootstrapContext = context.getBootstrapContext();
		CompositePropertySource compositePropertySource = locate(bootstrapContext, resource);
		return new ConfigData(compositePropertySource.getPropertySources(),
				getOptions(context, resource));
	}

	private CompositePropertySource locate(ConfigurableBootstrapContext bootstrapContext,
									 PolarisConfigDataResource resource) {
		CompositePropertySource compositePropertySource = new CompositePropertySource(
				POLARIS_CONFIG_PROPERTY_SOURCE_NAME);

		// load spring boot default config files
		initInternalConfigFiles(compositePropertySource, bootstrapContext,resource);

		PolarisConfigProperties polarisConfigProperties = bootstrapContext.get(PolarisConfigProperties.class);

		// load custom config files
		List<ConfigFileGroup> configFileGroups = polarisConfigProperties.getGroups();
		if (CollectionUtils.isEmpty(configFileGroups)) {
			return compositePropertySource;
		}
		initCustomPolarisConfigFiles(compositePropertySource, configFileGroups, bootstrapContext);

		return compositePropertySource;
	}

	private ConfigData.Option[] getOptions(ConfigDataLoaderContext context,
										   PolarisConfigDataResource resource) {
		List<ConfigData.Option> options = new ArrayList<>();
		options.add(IGNORE_IMPORTS);
		options.add(IGNORE_PROFILES);
		// mark it as 'PROFILE_SPECIFIC' config, it has higher priority,
		// will override the none profile specific config.
//		options.add(PROFILE_SPECIFIC);
		return options.toArray(new ConfigData.Option[0]);
	}

	private void initInternalConfigFiles(CompositePropertySource compositePropertySource,
										 ConfigurableBootstrapContext bootstrapContext,
										 PolarisConfigDataResource resource) {
		PolarisPropertySourceManager polarisPropertySourceManager =
				bootstrapContext.get(PolarisPropertySourceManager.class);
		List<ConfigFileMetadata> internalConfigFiles = getInternalConfigFiles(bootstrapContext,resource);

		for (ConfigFileMetadata configFile : internalConfigFiles) {
			PolarisPropertySource polarisPropertySource = loadPolarisPropertySource(bootstrapContext,
					configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName());

			compositePropertySource.addPropertySource(polarisPropertySource);

			polarisPropertySourceManager.addPropertySource(polarisPropertySource);

			log.info("[SCT Config] Load and inject polaris config file. file = " + configFile);
		}
	}

	private List<ConfigFileMetadata> getInternalConfigFiles(ConfigurableBootstrapContext bootstrapContext,
															PolarisConfigDataResource resource) {
		PolarisContextProperties polarisContextProperties = bootstrapContext.get(PolarisContextProperties.class);
		String namespace = polarisContextProperties.getNamespace();
		String serviceName = polarisContextProperties.getService();
		if (!StringUtils.hasText(serviceName)) {
			serviceName = resource.getServiceName();
		}

		List<ConfigFileMetadata> internalConfigFiles = new LinkedList<>();

		// priority: application-${profile} > application > boostrap-${profile} > boostrap
		List<String> activeProfiles = resource.getProfiles().getActive();

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


	private void initCustomPolarisConfigFiles(CompositePropertySource compositePropertySource,
											  List<ConfigFileGroup> configFileGroups,
											  ConfigurableBootstrapContext bootstrapContext
											  ) {
		PolarisContextProperties polarisContextProperties = bootstrapContext.get(PolarisContextProperties.class);
		String namespace = polarisContextProperties.getNamespace();
		PolarisPropertySourceManager polarisPropertySourceManager =
				bootstrapContext.get(PolarisPropertySourceManager.class);
		for (ConfigFileGroup configFileGroup : configFileGroups) {
			String group = configFileGroup.getName();

			if (!StringUtils.hasText(group)) {
				throw new IllegalArgumentException("polaris config group name cannot be empty.");
			}

			List<String> files = configFileGroup.getFiles();
			if (CollectionUtils.isEmpty(files)) {
				return;
			}

			for (String fileName : files) {
				PolarisPropertySource polarisPropertySource = loadPolarisPropertySource(bootstrapContext,
						namespace, group, fileName);

				compositePropertySource.addPropertySource(polarisPropertySource);

				polarisPropertySourceManager.addPropertySource(polarisPropertySource);

				String loggerFormat = "[SCT Config] Load and inject polaris config file success. " +
						"namespace = %s, group = %s, fileName = %s";
				log.info(String.format(loggerFormat,namespace, group, fileName));
			}
		}
	}

	private PolarisPropertySource loadPolarisPropertySource(
			ConfigurableBootstrapContext bootstrapContext,
			String namespace, String group, String fileName) {

		SDKContext sdkContext = bootstrapContext.get(SDKContext.class);

		ConfigKVFile configKVFile;
		ConfigFileService configFileService = ConfigFileServiceFactory.createConfigFileService(sdkContext);

		// unknown extension is resolved as properties file
		if (ConfigFileFormat.isPropertyFile(fileName)
				|| ConfigFileFormat.isUnknownFile(fileName)) {
			configKVFile = configFileService.getConfigPropertiesFile(namespace, group, fileName);
		}
		else if (ConfigFileFormat.isYamlFile(fileName)) {
			configKVFile = configFileService.getConfigYamlFile(namespace, group, fileName);
		}
		else {
			String loggerFormat = "[SCT Config] Unsupported config file. namespace = %s, group = %s, fileName = %s";
			log.warn(String.format(loggerFormat,namespace, group, fileName) );
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
