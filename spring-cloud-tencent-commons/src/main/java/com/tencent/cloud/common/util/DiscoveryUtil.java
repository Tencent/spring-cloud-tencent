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

public class DiscoveryUtil {

	/**
	 * rewrite serviceId when open double registry and discovery by nacos and polaris
	 *
	 * @param serviceId service id
	 * @return new service id
	 */
	public static String rewriteServiceId(String serviceId) {
		String enableNacos = ApplicationContextAwareUtils.getProperties("spring.cloud.nacos.enabled");
		String enableNacosDiscovery = ApplicationContextAwareUtils.getProperties("spring.cloud.nacos.discovery.enabled");
		String enableNacosRegistry = ApplicationContextAwareUtils.getProperties("spring.cloud.nacos.discovery.register-enabled");
		String enablePolarisDiscovery = ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.discovery.enabled");
		if (Boolean.parseBoolean(enableNacos)) {
			boolean rewrite = false;
			if (Boolean.parseBoolean(enableNacosRegistry) && Boolean.parseBoolean(enablePolarisDiscovery)) {
				rewrite = true;
			}
			if (Boolean.parseBoolean(enableNacosDiscovery) || Boolean.parseBoolean(enablePolarisDiscovery)) {
				rewrite = true;
			}
			if (rewrite) {
				String group = ApplicationContextAwareUtils.getProperties("spring.cloud.nacos.discovery.group",
						"DEFAULT_GROUP");
				serviceId = group + "__" + serviceId;
			}
		}
		return serviceId;
	}

}
