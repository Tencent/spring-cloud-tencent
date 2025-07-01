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

package com.tencent.cloud.polaris.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.LosslessAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.assembly.factory.AssemblyAPIFactory;
import com.tencent.polaris.auth.api.core.AuthAPI;
import com.tencent.polaris.auth.factory.AuthAPIFactory;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import com.tencent.polaris.router.api.core.RouterAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.Environment;

/**
 * Manager for static Polaris SDK context.
 *
 * @author Haotian Zhang
 */
public class PolarisSDKContextManager {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisSDKContextManager.class);

	/**
	 * Constant for checking before destroy SDK context.
	 */
	public volatile static boolean isRegistered = false;
	private volatile static SDKContext configSDKContext;
	private volatile static SDKContext serviceSdkContext;
	private volatile static ProviderAPI providerAPI;
	private volatile static ConsumerAPI consumerAPI;
	private volatile static LosslessAPI losslessAPI;
	private volatile static RouterAPI routerAPI;
	private volatile static CircuitBreakAPI circuitBreakAPI;
	private volatile static LimitAPI limitAPI;
	private volatile static AuthAPI authAPI;
	private volatile static AssemblyAPI assemblyAPI;
	private final PolarisContextProperties properties;
	private final Environment environment;
	private final List<PolarisConfigModifier> modifierList;

	public PolarisSDKContextManager(PolarisContextProperties properties, Environment environment, List<PolarisConfigModifier> modifierList) {
		this.properties = properties;
		this.environment = environment;
		this.modifierList = modifierList;
	}

	/**
	 * Don't call this method directly.
	 */
	public static void innerDestroy() {
		if (Objects.nonNull(serviceSdkContext)) {
			try {
				// destroy ProviderAPI
				if (Objects.nonNull(providerAPI)) {
					((AutoCloseable) providerAPI).close();
					providerAPI = null;
				}

				// destroy LosslessAPI
				if (Objects.nonNull(losslessAPI)) {
					((AutoCloseable) losslessAPI).close();
					losslessAPI = null;
				}

				// destroy ConsumerAPI
				if (Objects.nonNull(consumerAPI)) {
					((AutoCloseable) consumerAPI).close();
					consumerAPI = null;
				}

				// destroy RouterAPI
				if (Objects.nonNull(routerAPI)) {
					((Destroyable) routerAPI).destroy();
					routerAPI = null;
				}

				// destroy CircuitBreakAPI
				if (Objects.nonNull(circuitBreakAPI)) {
					((Destroyable) circuitBreakAPI).destroy();
					circuitBreakAPI = null;
				}

				// destroy LimitAPI
				if (Objects.nonNull(limitAPI)) {
					((AutoCloseable) limitAPI).close();
					limitAPI = null;
				}

				// destroy AuthAPI
				if (Objects.nonNull(authAPI)) {
					((AutoCloseable) authAPI).close();
					authAPI = null;
				}

				// destroy AssemblyAPI
				if (Objects.nonNull(assemblyAPI)) {
					((Destroyable) assemblyAPI).destroy();
					assemblyAPI = null;
				}

				if (Objects.nonNull(serviceSdkContext)) {
					serviceSdkContext.destroy();
					serviceSdkContext = null;
				}
				LOG.info("Polaris SDK context is destroyed.");
			}
			catch (Throwable throwable) {
				LOG.error("destroy Polaris SDK context failed.", throwable);
			}
		}
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

	public void init() {
		initService();
		initConfig();
	}

	public SDKContext getSDKContext() {
		initService();
		return serviceSdkContext;
	}

	public ProviderAPI getProviderAPI() {
		initService();
		return providerAPI;
	}

	public LosslessAPI getLosslessAPI() {
		initService();
		return losslessAPI;
	}

	public ConsumerAPI getConsumerAPI() {
		initService();
		return consumerAPI;
	}

	public RouterAPI getRouterAPI() {
		initService();
		return routerAPI;
	}

	public CircuitBreakAPI getCircuitBreakAPI() {
		initService();
		return circuitBreakAPI;
	}

	public LimitAPI getLimitAPI() {
		initService();
		return limitAPI;
	}

	public AuthAPI getAuthAPI() {
		initService();
		return authAPI;
	}

	public AssemblyAPI getAssemblyAPI() {
		return assemblyAPI;
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
			Runtime.getRuntime().addShutdownHook(new Thread(PolarisSDKContextManager::innerConfigDestroy));
			LOG.info("create Polaris config SDK context successfully.");
		}
	}

	public void initService() {
		if (null == serviceSdkContext) {
			try {
				// get modifiers for service.
				List<PolarisConfigModifier> serviceModifierList = new ArrayList<>();
				for (PolarisConfigModifier modifier : modifierList) {
					if (!(modifier instanceof PolarisConfigurationConfigModifier)) {
						serviceModifierList.add(modifier);
					}
				}
				// init SDKContext
				Configuration configuration = properties.configuration(serviceModifierList,
						() -> environment.getProperty("spring.cloud.client.ip-address"),
						() -> environment.getProperty("spring.cloud.polaris.local-port", Integer.class, 0));
				serviceSdkContext = SDKContext.initContextByConfig(configuration);
				serviceSdkContext.init();

				// init ProviderAPI
				providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(serviceSdkContext);

				// init losslessAPI
				losslessAPI = DiscoveryAPIFactory.createLosslessAPIByContext(serviceSdkContext);

				// init ConsumerAPI
				consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(serviceSdkContext);

				// init RouterAPI
				routerAPI = RouterAPIFactory.createRouterAPIByContext(serviceSdkContext);

				// init CircuitBreakAPI
				circuitBreakAPI = CircuitBreakAPIFactory.createCircuitBreakAPIByContext(serviceSdkContext);

				// init LimitAPI
				limitAPI = LimitAPIFactory.createLimitAPIByContext(serviceSdkContext);

				// init AuthAPI
				authAPI = AuthAPIFactory.createAuthAPIByContext(serviceSdkContext);

				// init AssemblyAPI
				assemblyAPI = AssemblyAPIFactory.createAssemblyAPIByContext(serviceSdkContext);

				// add shutdown hook
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					long startTimestamp = System.currentTimeMillis();
					long delay = 0;
					while (true) {
						if (!isRegistered || delay >= 60000) {
							innerDestroy();
							break;
						}
						else {
							delay = System.currentTimeMillis() - startTimestamp;
						}
					}
				}));
				LOG.info("create Polaris SDK context successfully. properties: {}, configuration: {}", properties, configuration);
			}
			catch (Throwable throwable) {
				LOG.error("create Polaris SDK context failed. properties: {}, ", properties, throwable);
				throw throwable;
			}
		}
	}

	public void initConfig() {
		// get modifiers for configuration.
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
				Runtime.getRuntime().addShutdownHook(new Thread(PolarisSDKContextManager::innerConfigDestroy));
				LOG.info("create Polaris config SDK context successfully. properties: {}, configuration: {}", properties, configuration);
			}
			catch (Throwable throwable) {
				LOG.error("create Polaris config SDK context failed. properties: {}, ", properties, throwable);
				throw throwable;
			}
		}
	}
}
