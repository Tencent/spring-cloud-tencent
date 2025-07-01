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

package com.tencent.cloud.plugin.discovery.adapter.transformer;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.tencent.cloud.rpc.enhancement.transformer.RegistrationTransformer;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.utils.StringUtils;

import org.springframework.cloud.client.serviceregistry.Registration;

public class NacosRegistrationTransformer implements RegistrationTransformer {

	@Override
	public String getRegistry() {
		return "nacos";
	}

	@Override
	public void transformCustom(DefaultInstance instance, Registration registration) {
		if (registration instanceof NacosRegistration nacosRegistration) {
			NacosDiscoveryProperties nacosDiscoveryProperties = nacosRegistration.getNacosDiscoveryProperties();
			String namespace = nacosDiscoveryProperties.getNamespace();
			if (StringUtils.isBlank(namespace)) {
				namespace = "default";
			}
			instance.setNamespace(namespace);
		}
	}

}
