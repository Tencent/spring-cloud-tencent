/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.registry;

import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.polaris.api.utils.StringUtils;

/**
 * 服务注册时端口相关逻辑.
 *
 * @author Haotian Zhang
 */
public class NamespaceRegistrationCustomizer implements PolarisRegistrationCustomizer {

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	public NamespaceRegistrationCustomizer(PolarisDiscoveryProperties polarisDiscoveryProperties) {
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
	}

	@Override
	public void customize(PolarisRegistration registration) {
		String namespaceExports = polarisDiscoveryProperties.getNamespaceExports();
		if (StringUtils.isNotBlank(namespaceExports)) {
			registration.getMetadata().put("POLARIS_INTERNAL_NAMESPACE_EXPORTS", namespaceExports);
		}
	}
}
