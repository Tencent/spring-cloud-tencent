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

package com.tencent.cloud.polaris.context;

import java.util.List;
import java.util.Objects;

import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.assembly.factory.AssemblyAPIFactory;
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
	private volatile static SDKContext sdkContext;
	private volatile static ProviderAPI providerAPI;
	private volatile static ConsumerAPI consumerAPI;
	private volatile static RouterAPI routerAPI;
	private volatile static CircuitBreakAPI circuitBreakAPI;
	private volatile static LimitAPI limitAPI;
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
		if (Objects.nonNull(sdkContext)) {
			try {
				// destroy ProviderAPI
				if (Objects.nonNull(providerAPI)) {
					((AutoCloseable) providerAPI).close();
					providerAPI = null;
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

				// destroy AssemblyAPI
				if (Objects.nonNull(assemblyAPI)) {
					((Destroyable) assemblyAPI).destroy();
					assemblyAPI = null;
				}

				if (Objects.nonNull(sdkContext)) {
					sdkContext.destroy();
					sdkContext = null;
				}
				LOG.info("Polaris SDK context is destroyed.");
			}
			catch (Throwable throwable) {
				LOG.error("destroy Polaris SDK context failed.", throwable);
			}
		}
	}

	public void init() {
		if (null == sdkContext) {
			try {
				// init SDKContext
				sdkContext = SDKContext.initContextByConfig(properties.configuration(modifierList,
						() -> environment.getProperty("spring.cloud.client.ip-address"),
						() -> environment.getProperty("spring.cloud.polaris.local-port", Integer.class, 0)));
				sdkContext.init();

				// init ProviderAPI
				providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(sdkContext);

				// init ConsumerAPI
				consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);

				// init RouterAPI
				routerAPI = RouterAPIFactory.createRouterAPIByContext(sdkContext);

				// init CircuitBreakAPI
				circuitBreakAPI = CircuitBreakAPIFactory.createCircuitBreakAPIByContext(sdkContext);

				// init LimitAPI
				limitAPI = LimitAPIFactory.createLimitAPIByContext(sdkContext);

				// init AssemblyAPI
				assemblyAPI = AssemblyAPIFactory.createAssemblyAPIByContext(sdkContext);

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
				LOG.info("create Polaris SDK context successfully. properties: {}", properties);
			}
			catch (Throwable throwable) {
				LOG.error("create Polaris SDK context failed. properties: {}", properties, throwable);
				throw throwable;
			}
		}
	}

	public SDKContext getSDKContext() {
		init();
		return sdkContext;
	}

	public ProviderAPI getProviderAPI() {
		init();
		return providerAPI;
	}

	public ConsumerAPI getConsumerAPI() {
		init();
		return consumerAPI;
	}

	public RouterAPI getRouterAPI() {
		init();
		return routerAPI;
	}

	public CircuitBreakAPI getCircuitBreakAPI() {
		init();
		return circuitBreakAPI;
	}

	public LimitAPI getLimitAPI() {
		init();
		return limitAPI;
	}

	public AssemblyAPI getAssemblyAPI() {
		return assemblyAPI;
	}
}
