package com.tencent.cloud.polaris.config.configdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.tencent.cloud.polaris.config.ConfigurationModifier;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.context.ModifyAddress;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.api.config.ConfigProvider;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import org.apache.commons.logging.Log;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;

/**
 * Implementation of {@link ConfigDataLocationResolver}, used to resolve {@link ConfigDataLocation locations}
 * into one or more {@link PolarisConfigDataResource polarisConfigDataResource}.
 *
 * @author wlx
 */
public class PolarisConfigDataLocationResolver implements
		ConfigDataLocationResolver<PolarisConfigDataResource>, Ordered {


	/**
	 * Prefix for Config Server imports.
	 */
	public static final String PREFIX = "polaris";

	/**
	 * Prefix for Polaris configurationProperties.
	 */
	public static final String POLARIS_PREFIX = "spring.cloud.polaris";

	/**
	 * COLON.
	 */
	public static final String COLON = ":";

	/**
	 * Empty String.
	 */
	public static final String EMPTY_STRING = "";

	private final Log log;

	public PolarisConfigDataLocationResolver(DeferredLogFactory logFactory) {
		this.log = logFactory.getLog(getClass());
	}

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		if (!location.hasPrefix(PREFIX)) {
			return false;
		}
		return context.getBinder()
				.bind("spring.cloud.polaris.config.enabled", Boolean.class)
				.orElse(true);
	}

	@Override
	public List<PolarisConfigDataResource> resolve(
			ConfigDataLocationResolverContext context, ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException,
			ConfigDataResourceNotFoundException {
		return Collections.emptyList();
	}


	@Override
	public List<PolarisConfigDataResource> resolveProfileSpecific(
			ConfigDataLocationResolverContext resolverContext,
			ConfigDataLocation location, Profiles profiles)
			throws ConfigDataLocationNotFoundException {

		ConfigurableBootstrapContext bootstrapContext = resolverContext.getBootstrapContext();

		PolarisConfigProperties polarisConfigProperties = loadPolarisConfigProperties(
				resolverContext,
				PolarisConfigProperties.class,
				POLARIS_PREFIX + ".config"
		);

		PolarisContextProperties polarisContextProperties = loadPolarisConfigProperties(
				resolverContext,
				PolarisContextProperties.class,
				POLARIS_PREFIX
		);

		// prepare and init earlier Polaris SDKContext to pull config files from remote.
		prepareAndInitEarlierPolarisSdkContext(resolverContext, polarisConfigProperties, polarisContextProperties);

		bootstrapContext.registerIfAbsent(PolarisConfigProperties.class,
				BootstrapRegistry.InstanceSupplier.of(polarisConfigProperties));

		bootstrapContext.registerIfAbsent(PolarisContextProperties.class,
				BootstrapRegistry.InstanceSupplier.of(polarisContextProperties));

		bootstrapContext.registerIfAbsent(PolarisPropertySourceManager.class,
				BootstrapRegistry.InstanceSupplier.of(new PolarisPropertySourceManager()));

		bootstrapContext.addCloseListener(
				event -> {
					// destroy earlier Polaris sdkContext
					event.getBootstrapContext().get(SDKContext.class).destroy();
					// register PolarisPropertySourceManager to context
					PolarisPropertySourceManager polarisPropertySourceManager = event.getBootstrapContext().get(PolarisPropertySourceManager.class);
					event.getApplicationContext().getBeanFactory().registerSingleton(
							"polarisPropertySourceManager", polarisPropertySourceManager);
				}
		);

		return loadConfigDataResources(resolverContext,
				location, profiles, polarisConfigProperties, polarisContextProperties);
	}

	@Override
	public int getOrder() {
		return -1;
	}

	protected <T> T loadPolarisConfigProperties(
			ConfigDataLocationResolverContext context,
			Class<T> typeClass,
			String prefix) {
		Binder binder = context.getBinder();
		BindHandler bindHandler = getBindHandler(context);

		T instance;
		if (context.getBootstrapContext().isRegistered(typeClass)) {
			instance = context.getBootstrapContext().get(typeClass);
		}
		else {
			instance = binder.bind(prefix, Bindable.of(typeClass), bindHandler)
					.map(properties -> binder.bind(prefix, Bindable.ofInstance(properties), bindHandler)
							.orElse(properties))
					.orElseGet(() -> binder.bind(prefix, Bindable.of(typeClass), bindHandler)
							.orElseGet(null));
		}
		return instance;
	}

	private BindHandler getBindHandler(ConfigDataLocationResolverContext context) {
		return context.getBootstrapContext().getOrElse(BindHandler.class, null);
	}

	private List<PolarisConfigDataResource> loadConfigDataResources(ConfigDataLocationResolverContext resolverContext,
																	ConfigDataLocation location,
																	Profiles profiles,
																	PolarisConfigProperties polarisConfigProperties,
																	PolarisContextProperties polarisContextProperties) {
		List<PolarisConfigDataResource> result = new ArrayList<>();
		boolean optional = location.isOptional();
		String groupFileName = getRealGroupFileName(location);
		String serviceName = loadPolarisConfigProperties(resolverContext,
				String.class, "spring.application.name");
		String groupName = StringUtils.isBlank(groupFileName) ? EMPTY_STRING : parseGroupName(groupFileName, serviceName);
		log.info("group from configDataLocation is" + groupName);
		String fileName = StringUtils.isBlank(groupFileName) ? EMPTY_STRING : parseFileName(groupFileName);
		log.info("file from configDataLocation is" + fileName);
		PolarisConfigDataResource polarisConfigDataResource = new PolarisConfigDataResource(
				polarisConfigProperties,
				polarisContextProperties,
				profiles, optional,
				fileName, groupName, serviceName
		);
		result.add(polarisConfigDataResource);
		return result;
	}

	private String getRealGroupFileName(ConfigDataLocation location) {
		String prefixedValue = location.getNonPrefixedValue(PREFIX);
		if (StringUtils.isBlank(prefixedValue) || !prefixedValue.startsWith(COLON)) {
			return prefixedValue;
		}
		return prefixedValue.substring(1);
	}

	private String parseFileName(String groupFileName) {
		String[] split = groupFileName.split(COLON);
		if (split.length > 1) {
			return split[1];
		}
		else {
			return split[0];
		}
	}

	private String parseGroupName(String groupFileName, String serviceName) {
		String[] split = groupFileName.split(COLON);
		if (split.length > 1) {
			return split[0];
		}
		else {
			return serviceName;
		}
	}

	private void prepareAndInitEarlierPolarisSdkContext(ConfigDataLocationResolverContext resolverContext,
														PolarisConfigProperties polarisConfigProperties,
														PolarisContextProperties polarisContextProperties) {
		ConfigurableBootstrapContext bootstrapContext = resolverContext.getBootstrapContext();
		if (!bootstrapContext.isRegistered(SDKContext.class)) {
			SDKContext sdkContext = sdkContext(resolverContext,
					polarisConfigProperties, polarisContextProperties);
			sdkContext.init();
			bootstrapContext.register(SDKContext.class, BootstrapRegistry.InstanceSupplier.of(sdkContext));
		}

	}

	private SDKContext sdkContext(ConfigDataLocationResolverContext resolverContext,
			PolarisConfigProperties polarisConfigProperties,
			PolarisContextProperties polarisContextProperties) {

		// 1. Read user-defined polaris.yml configuration
		ConfigurationImpl configuration = (ConfigurationImpl) ConfigAPIFactory
				.defaultConfig(ConfigProvider.DEFAULT_CONFIG);

		// 2. Override user-defined polaris.yml configuration with SCT configuration

		String defaultHost = polarisContextProperties.getLocalIpAddress();
		if (StringUtils.isBlank(defaultHost)) {
			defaultHost = loadPolarisConfigProperties(resolverContext, String.class, "spring.cloud.client.ip-address");
		}

		configuration.getGlobal().getAPI().setBindIP(defaultHost);

		Collection<PolarisConfigModifier> modifiers = modifierList(polarisConfigProperties, polarisContextProperties);
		modifiers = modifiers.stream()
				.sorted(Comparator.comparingInt(PolarisConfigModifier::getOrder))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(modifiers)) {
			for (PolarisConfigModifier modifier : modifiers) {
				modifier.modify(configuration);
			}
		}
		return SDKContext.initContextByConfig(configuration);
	}

	private List<PolarisConfigModifier> modifierList(PolarisConfigProperties polarisConfigProperties,
			PolarisContextProperties polarisContextProperties) {
		// add ModifyAddress and ConfigurationModifier to load SDKContext
		List<PolarisConfigModifier> modifierList = new ArrayList<>();
		ModifyAddress modifyAddress = new ModifyAddress();
		modifyAddress.setProperties(polarisContextProperties);

		ConfigurationModifier configurationModifier = new ConfigurationModifier(polarisConfigProperties,
				polarisContextProperties);
		modifierList.add(modifyAddress);
		modifierList.add(configurationModifier);
		return modifierList;
	}
}

