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

package com.tencent.cloud.polaris.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.PolarisConfigurationConfigModifier;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.api.SDKContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

/**
 * Manager for static Polaris Config SDK context.
 *
 * @author Haotian Zhang
 */
public class PolarisConfigSDKContextManager {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisConfigSDKContextManager.class);

	private volatile static SDKContext configSDKContext;
	private final PolarisContextProperties properties;
	private final Environment environment;
	private final ObjectProvider<List<PolarisConfigModifier>> modifierListProvider;

	public PolarisConfigSDKContextManager(PolarisContextProperties properties, Environment environment, ObjectProvider<List<PolarisConfigModifier>> modifierListProvider) {
		this.properties = properties;
		this.environment = environment;
		this.modifierListProvider = modifierListProvider;
	}

	/**
	 * Used for config data.
	 */
	public static SDKContext innerGetConfigSDKContext() {
		if (configSDKContext == null) {
			throw new IllegalArgumentException("configSDKContext is not initialized.");
		}
		return configSDKContext;
	}

	public static void innerConfigDestroy() {
		try {
			if (Objects.nonNull(configSDKContext)) {
				configSDKContext.destroy();
				configSDKContext = null;
			}
			LOG.info("Polaris SDK config context is destroyed.");
		}
		catch (Throwable throwable) {
			LOG.info("Polaris SDK config context is destroyed failed.", throwable);
		}
	}

	public SDKContext getConfigSDKContext() {
		initConfig();
		return configSDKContext;
	}

	/**
	 * Used for config data.
	 */
	public static void setConfigSDKContext(SDKContext context) {
		if (configSDKContext == null) {
			configSDKContext = context;
			// add shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread(PolarisConfigSDKContextManager::innerConfigDestroy));
			LOG.info("create Polaris config SDK context successfully.");
		}
	}

	public void initConfig() {
		// get modifiers for configuration.
		List<PolarisConfigModifier> modifierList = modifierListProvider.getIfAvailable(ArrayList::new);
		List<PolarisConfigModifier> configModifierList = new ArrayList<>();
		for (PolarisConfigModifier modifier : modifierList) {
			if (modifier instanceof PolarisConfigurationConfigModifier) {
				configModifierList.add(modifier);
			}
		}
		if (null == configSDKContext && CollectionUtils.isNotEmpty(configModifierList)) {
			try {
				// init config SDKContext
				Configuration configuration = properties.configuration(configModifierList,
						() -> environment.getProperty("spring.cloud.client.ip-address"),
						() -> environment.getProperty("spring.cloud.polaris.local-port", Integer.class, 0));
				configSDKContext = SDKContext.initContextByConfig(configuration);
				configSDKContext.init();

				// add shutdown hook
				Runtime.getRuntime().addShutdownHook(new Thread(PolarisConfigSDKContextManager::innerConfigDestroy));
				LOG.info("create Polaris config SDK context successfully. properties: {}, configuration: {}", properties, configuration);
			}
			catch (Throwable throwable) {
				LOG.error("create Polaris config SDK context failed. properties: {}, ", properties, throwable);
				throw throwable;
			}
		}
	}
}
