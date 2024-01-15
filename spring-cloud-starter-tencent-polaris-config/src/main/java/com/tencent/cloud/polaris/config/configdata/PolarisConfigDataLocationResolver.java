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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.tencent.cloud.polaris.config.ConfigurationModifier;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.config.PolarisCryptoConfigProperties;
import com.tencent.cloud.polaris.context.ModifyAddress;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
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
		if (Objects.isNull(polarisConfigProperties)) {
			polarisConfigProperties = new PolarisConfigProperties();
		}

		PolarisCryptoConfigProperties polarisCryptoConfigProperties = loadPolarisConfigProperties(
				resolverContext,
				PolarisCryptoConfigProperties.class,
				POLARIS_PREFIX + ".config.crypto"
		);
		if (Objects.isNull(polarisCryptoConfigProperties)) {
			polarisCryptoConfigProperties = new PolarisCryptoConfigProperties();
		}

		PolarisContextProperties polarisContextProperties = loadPolarisConfigProperties(
				resolverContext,
				PolarisContextProperties.class,
				POLARIS_PREFIX
		);
		if (Objects.isNull(polarisContextProperties)) {
			polarisContextProperties = new PolarisContextProperties();
		}

		// prepare and init earlier Polaris SDKContext to pull config files from remote.
		prepareAndInitEarlierPolarisSdkContext(resolverContext, polarisConfigProperties, polarisCryptoConfigProperties, polarisContextProperties);

		bootstrapContext.registerIfAbsent(PolarisConfigProperties.class,
				BootstrapRegistry.InstanceSupplier.of(polarisConfigProperties));

		bootstrapContext.registerIfAbsent(PolarisCryptoConfigProperties.class,
				BootstrapRegistry.InstanceSupplier.of(polarisCryptoConfigProperties));

		bootstrapContext.registerIfAbsent(PolarisContextProperties.class,
				BootstrapRegistry.InstanceSupplier.of(polarisContextProperties));

		bootstrapContext.registerIfAbsent(PolarisPropertySourceManager.class,
				BootstrapRegistry.InstanceSupplier.of(new PolarisPropertySourceManager()));

		bootstrapContext.addCloseListener(
				event -> {
					// destroy earlier Polaris sdkContext
					event.getBootstrapContext().get(SDKContext.class).destroy();
					// register PolarisPropertySourceManager to context
					PolarisPropertySourceManager polarisPropertySourceManager = event.getBootstrapContext()
							.get(PolarisPropertySourceManager.class);
					event.getApplicationContext().getBeanFactory().registerSingleton(
							"polarisPropertySourceManager", polarisPropertySourceManager);
				}
		);

		return loadConfigDataResources(resolverContext,
				location, profiles, polarisConfigProperties, polarisCryptoConfigProperties, polarisContextProperties);
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
		if (!registerNotNecessary(typeClass) && context.getBootstrapContext().isRegistered(typeClass)) {
			instance = context.getBootstrapContext().get(typeClass);
		}
		else {
			instance = binder.bind(prefix, Bindable.of(typeClass), bindHandler)
					.map(properties -> binder.bind(prefix, Bindable.ofInstance(properties), bindHandler)
							.orElse(properties))
					.orElseGet(() -> binder.bind(prefix, Bindable.of(typeClass), bindHandler)
							.orElseGet(() -> null));
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
			PolarisCryptoConfigProperties polarisCryptoConfigProperties,
			PolarisContextProperties polarisContextProperties) {
		List<PolarisConfigDataResource> result = new ArrayList<>();
		boolean optional = location.isOptional();
		String groupFileName = getRealGroupFileName(location);
		String serviceName = loadPolarisConfigProperties(resolverContext,
				String.class, "spring.application.name");
		if (StringUtils.isBlank(serviceName)) {
			serviceName = "application";
			log.warn("No spring.application.name found, defaulting to 'application'");
		}
		String groupName = StringUtils.isBlank(groupFileName) ? EMPTY_STRING : parseGroupName(groupFileName, serviceName);
		if (StringUtils.isNotBlank(groupName)) {
			log.info("group from configDataLocation is " + groupName);
		}
		String fileName = StringUtils.isBlank(groupFileName) ? EMPTY_STRING : parseFileName(groupFileName);
		if (StringUtils.isNotBlank(fileName)) {
			log.info("file from configDataLocation is " + fileName);
		}
		PolarisConfigDataResource polarisConfigDataResource = new PolarisConfigDataResource(
				polarisConfigProperties,
				polarisCryptoConfigProperties,
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
			PolarisConfigProperties polarisConfigProperties, PolarisCryptoConfigProperties polarisCryptoConfigProperties,
			PolarisContextProperties polarisContextProperties) {
		ConfigurableBootstrapContext bootstrapContext = resolverContext.getBootstrapContext();
		if (!bootstrapContext.isRegistered(SDKContext.class)) {
			SDKContext sdkContext = sdkContext(resolverContext,
					polarisConfigProperties, polarisCryptoConfigProperties, polarisContextProperties);
			// not init reporter when creating config data temp SDK context.
			if (sdkContext.getConfig() instanceof ConfigurationImpl) {
				((ConfigurationImpl) sdkContext.getConfig()).getGlobal().getStatReporter().setEnable(false);
			}
			sdkContext.init();
			bootstrapContext.register(SDKContext.class, BootstrapRegistry.InstanceSupplier.of(sdkContext));
		}

	}

	private SDKContext sdkContext(ConfigDataLocationResolverContext resolverContext,
			PolarisConfigProperties polarisConfigProperties, PolarisCryptoConfigProperties polarisCryptoConfigProperties,
			PolarisContextProperties polarisContextProperties) {
		List<PolarisConfigModifier> modifierList = modifierList(polarisConfigProperties, polarisCryptoConfigProperties, polarisContextProperties);
		return SDKContext.initContextByConfig(polarisContextProperties.configuration(modifierList, () -> {
			return loadPolarisConfigProperties(resolverContext, String.class, "spring.cloud.client.ip-address");
		}, () -> {
			return loadPolarisConfigProperties(resolverContext, Integer.class, "spring.cloud.polaris.local-port");
		}));
	}

	private List<PolarisConfigModifier> modifierList(PolarisConfigProperties polarisConfigProperties,
			PolarisCryptoConfigProperties polarisCryptoConfigProperties,
			PolarisContextProperties polarisContextProperties) {
		// add ModifyAddress and ConfigurationModifier to load SDKContext
		List<PolarisConfigModifier> modifierList = new ArrayList<>();
		ModifyAddress modifyAddress = new ModifyAddress(polarisContextProperties);

		ConfigurationModifier configurationModifier = new ConfigurationModifier(polarisConfigProperties,
				polarisCryptoConfigProperties, polarisContextProperties);
		modifierList.add(modifyAddress);
		modifierList.add(configurationModifier);
		return modifierList;
	}

	private boolean registerNotNecessary(Class<?> typeClass) {
		return typeClass.isPrimitive() ||
				Number.class.isAssignableFrom(typeClass) ||
				String.class.isAssignableFrom(typeClass) ||
				Character.class.isAssignableFrom(typeClass) ||
				Boolean.class.isAssignableFrom(typeClass);
	}
}

