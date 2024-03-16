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

package com.tencent.cloud.plugin.lossless.transfomer;

import java.lang.reflect.Method;
import java.util.Properties;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.client.naming.utils.InitUtils;

import org.springframework.util.ReflectionUtils;

/**
 * Discovery namespace getter for Nacos.
 *
 * @author Shedfree Wu
 */
public class NacosDiscoveryNamespaceGetter implements DiscoveryNamespaceGetter {
	private String namespace;

	public NacosDiscoveryNamespaceGetter(NacosDiscoveryProperties nacosDiscoveryProperties) {
		// getNacosProperties is private in low version of spring-cloud-starter-alibaba-nacos-discovery
		Method method = ReflectionUtils.findMethod(NacosDiscoveryProperties.class, "getNacosProperties");
		method.setAccessible(true);
		this.namespace = InitUtils.initNamespaceForNaming(
				(Properties) ReflectionUtils.invokeMethod(method, nacosDiscoveryProperties));
	}

	public String getNamespace() {
		return namespace;
	}
}
