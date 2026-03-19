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
import java.util.Objects;

import com.tencent.cloud.polaris.config.ConfigurationModifier;
import com.tencent.cloud.polaris.config.PolarisConfigSDKContextManager;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.config.PolarisCryptoConfigProperties;
import com.tencent.cloud.polaris.context.ModifyAddress;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfTlsProperties;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import org.apache.commons.logging.Log;

import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
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

		boolean contextEnabled = context.getBinder()
				.bind("spring.cloud.polaris.enabled", Boolean.class)
				.orElse(true);

		boolean configEnabled = context.getBinder()
				.bind("spring.cloud.polaris.config.enabled", Boolean.class)
				.orElse(true);

		return contextEnabled && configEnabled;
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

		if (!polarisContextProperties.getEnabled() || !polarisConfigProperties.isEnabled()) {
			return Collections.emptyList();
		}

		TsfTlsProperties tsfTlsProperties = loadPolarisConfigProperties(
				resolverContext,
				TsfTlsProperties.class,
				POLARIS_PREFIX + ".tls");
		if (Objects.isNull(tsfTlsProperties)) {
			tsfTlsProperties = new TsfTlsProperties();
		}

		// prepare and init earlier Polaris SDKContext to pull config files from remote.
		try {
			prepareAndInitEarlierPolarisSdkContext(resolverContext, polarisConfigProperties, polarisCryptoConfigProperties, polarisContextProperties);
		}
		catch (Throwable throwable) {
			if (location.isOptional()) {
				log.warn("create earlier polaris SDK context failed.", throwable);
				return new ArrayList<>();
			}
			else {
				log.error("create earlier polaris SDK context failed.", throwable);
				throw throwable;
			}
		}
		bootstrapContext.registerIfAbsent(PolarisConfigProperties.class,
				BootstrapRegistry.InstanceSupplier.of(polarisConfigProperties));

		bootstrapContext.registerIfAbsent(PolarisCryptoConfigProperties.class,
				BootstrapRegistry.InstanceSupplier.of(polarisCryptoConfigProperties));

		bootstrapContext.registerIfAbsent(PolarisContextProperties.class,
				BootstrapRegistry.InstanceSupplier.of(polarisContextProperties));

		bootstrapContext.registerIfAbsent(TsfTlsProperties.class,
				BootstrapRegistry.InstanceSupplier.of(tsfTlsProperties));

		return loadConfigDataResources(resolverContext,
				location, profiles, polarisConfigProperties, polarisCryptoConfigProperties, polarisContextProperties, tsfTlsProperties);
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
			PolarisContextProperties polarisContextProperties,
			TsfTlsProperties tsfTlsProperties) {
		List<PolarisConfigDataResource> result = new ArrayList<>();
		boolean optional = location.isOptional();
		String groupFileName = getRealGroupFileName(location);
		String serviceName = loadPolarisConfigProperties(resolverContext,
				String.class, "spring.cloud.polaris.discovery.service");
		if (StringUtils.isBlank(serviceName)) {
			serviceName = loadPolarisConfigProperties(resolverContext,
					String.class, "spring.cloud.polaris.service");
		}
		if (StringUtils.isBlank(serviceName)) {
			serviceName = loadPolarisConfigProperties(resolverContext,
					String.class, "spring.application.name");
		}
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
				tsfTlsProperties,
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
			SDKContext sdkContext;
			try {
				sdkContext = PolarisConfigSDKContextManager.innerGetConfigSDKContext();
			}
			catch (IllegalArgumentException e) {
				sdkContext = sdkContext(resolverContext,
						polarisConfigProperties, polarisCryptoConfigProperties, polarisContextProperties);
				if (sdkContext.getConfig() instanceof ConfigurationImpl) {
					// not init reporter when creating config data temp SDK context.
					((ConfigurationImpl) sdkContext.getConfig()).getGlobal().getStatReporter().setEnable(false);
					// not init circuit breaker when creating config data temp SDK context.
					((ConfigurationImpl) sdkContext.getConfig()).getConsumer().getCircuitBreaker().setEnable(false);
				}
				sdkContext.init();
				PolarisConfigSDKContextManager.setConfigSDKContext(sdkContext);
			}
		}

	}

	private SDKContext sdkContext(ConfigDataLocationResolverContext resolverContext,
			PolarisConfigProperties polarisConfigProperties, PolarisCryptoConfigProperties polarisCryptoConfigProperties,
			PolarisContextProperties polarisContextProperties) {
		List<PolarisConfigModifier> modifierList = modifierList(polarisConfigProperties, polarisCryptoConfigProperties, polarisContextProperties);
		return SDKContext.initContextByConfig(polarisContextProperties.configuration(
				modifierList,
				() -> loadPolarisConfigProperties(resolverContext, String.class, "spring.cloud.client.ip-address"),
				() -> loadPolarisConfigProperties(resolverContext, Integer.class, "spring.cloud.polaris.local-port")));
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

