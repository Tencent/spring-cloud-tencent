package com.tencent.cloud.polaris.config.configdata;

import com.tencent.cloud.polaris.config.adapter.PolarisConfigFilePuller;
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

		SDKContext sdkContext = bootstrapContext.get(SDKContext.class);
		ConfigFileService configFileService = ConfigFileServiceFactory.createConfigFileService(sdkContext);
		PolarisConfigFilePuller puller = new PolarisConfigFilePuller(resource.getPolarisContextProperties()
				,configFileService,
				bootstrapContext.get(PolarisPropertySourceManager.class));
		puller.initCustomPolarisConfigFile(compositePropertySource, configFileGroup(resource));
		return compositePropertySource;
	}

	private ConfigData.Option[] getOptions(ConfigDataLoaderContext context,
										   PolarisConfigDataResource resource) {
		List<ConfigData.Option> options = new ArrayList<>();
		options.add(IGNORE_IMPORTS);
		options.add(IGNORE_PROFILES);
		// mark it as 'PROFILE_SPECIFIC' config, it has higher priority,
		// will override the none profile specific config.
		options.add(PROFILE_SPECIFIC);
		return options.toArray(new ConfigData.Option[0]);
	}

	private ConfigFileGroup configFileGroup(PolarisConfigDataResource polarisConfigDataResource) {
		String fileName = polarisConfigDataResource.getFileName();
		String serviceName = polarisConfigDataResource.getServiceName();
		ConfigFileGroup configFileGroup = new ConfigFileGroup();
		configFileGroup.setName(serviceName);
		List<String> flies = new ArrayList<>();
		flies.add(fileName);
		configFileGroup.setFiles(flies);
		return configFileGroup;
	}

}
