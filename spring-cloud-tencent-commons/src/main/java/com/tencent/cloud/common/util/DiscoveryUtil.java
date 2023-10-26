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

package com.tencent.cloud.common.util;

/**
 * Utils for Discovery.
 */
public final class DiscoveryUtil {

	private static String ENABLE_NACOS;

	private static String ENABLE_NACOS_DISCOVERY;

	private static String ENABLE_NACOS_REGISTRY;

	private static String ENABLE_POLARIS_DISCOVERY;

	private static String NACOS_GROUP;

	private static String NACOS_NAMESPACE;

	private static final Object MUTEX = new Object();

	private static boolean INITIALIZE = false;

	private DiscoveryUtil() {
	}

	/**
	 * rewrite serviceId when open double registry and discovery by nacos and polaris.
	 *
	 * @param serviceId service id
	 * @return new service id
	 */
	public static String rewriteServiceId(String serviceId) {
		init();
		if (Boolean.parseBoolean(ENABLE_NACOS)) {
			boolean rewrite = false;
			if (Boolean.parseBoolean(ENABLE_NACOS_REGISTRY) && Boolean.parseBoolean(ENABLE_POLARIS_DISCOVERY)) {
				rewrite = true;
			}
			if (Boolean.parseBoolean(ENABLE_NACOS_DISCOVERY) || Boolean.parseBoolean(ENABLE_POLARIS_DISCOVERY)) {
				rewrite = true;
			}
			if (rewrite) {
				serviceId = NACOS_GROUP + "__" + serviceId;
			}
		}
		return serviceId;
	}

	/**
	 * rewrite namespace when open double registry and discovery by nacos and polaris.
	 *
	 * @param namespace namespace
	 * @return new namespace
	 */
	public static String rewriteNamespace(String namespace) {
		init();
		if (Boolean.parseBoolean(ENABLE_NACOS)) {
			boolean rewrite = false;
			if (Boolean.parseBoolean(ENABLE_NACOS_REGISTRY) && Boolean.parseBoolean(ENABLE_POLARIS_DISCOVERY)) {
				rewrite = true;
			}
			if (Boolean.parseBoolean(ENABLE_NACOS_DISCOVERY) || Boolean.parseBoolean(ENABLE_POLARIS_DISCOVERY)) {
				rewrite = true;
			}
			if (rewrite) {
				namespace = NACOS_NAMESPACE;
			}
		}
		return namespace;
	}

	private static void init() {
		if (INITIALIZE) {
			return;
		}
		synchronized (MUTEX) {
			if (INITIALIZE) {
				return;
			}
			ENABLE_NACOS = ApplicationContextAwareUtils.getProperties("spring.cloud.nacos.enabled");
			ENABLE_NACOS_DISCOVERY = ApplicationContextAwareUtils.getProperties("spring.cloud.nacos.discovery.enabled");
			ENABLE_NACOS_REGISTRY = ApplicationContextAwareUtils.getProperties("spring.cloud.nacos.discovery.register-enabled");
			ENABLE_POLARIS_DISCOVERY = ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.discovery.enabled");
			NACOS_GROUP = ApplicationContextAwareUtils.getProperties("spring.cloud.nacos.discovery.group", "DEFAULT_GROUP");
			NACOS_NAMESPACE = ApplicationContextAwareUtils.getProperties("spring.cloud.nacos.discovery.namespace", "public");
			INITIALIZE = true;
		}
	}

}
