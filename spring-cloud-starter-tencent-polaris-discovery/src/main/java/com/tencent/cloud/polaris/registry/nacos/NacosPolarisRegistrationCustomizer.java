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

package com.tencent.cloud.polaris.registry.nacos;

import java.util.Map;
import java.util.Objects;

import com.tencent.cloud.polaris.extend.nacos.NacosContextProperties;
import com.tencent.cloud.polaris.registry.PolarisRegistration;
import com.tencent.cloud.polaris.registry.PolarisRegistrationCustomizer;
import com.tencent.polaris.api.utils.StringUtils;


import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_CLUSTER_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.NACOS_GROUP_KEY;
import static shade.polaris.com.alibaba.nacos.api.common.Constants.DEFAULT_CLUSTER_NAME;
import static shade.polaris.com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;

public class NacosPolarisRegistrationCustomizer implements PolarisRegistrationCustomizer {
	NacosContextProperties nacosContextProperties;
	public NacosPolarisRegistrationCustomizer(NacosContextProperties nacosContextProperties) {
		this.nacosContextProperties = nacosContextProperties;
	}
	@Override
	public void customize(PolarisRegistration registration) {
		// put internal-nacos-cluster if necessary
		Map<String, String> instanceMetadata = registration.getMetadata();
		if (Objects.nonNull(nacosContextProperties)) {
			String clusterName = nacosContextProperties.getClusterName();
			if (StringUtils.isNotBlank(clusterName) && !DEFAULT_CLUSTER_NAME.equals(clusterName)) {
				instanceMetadata.put(NACOS_CLUSTER_KEY, clusterName);
			}
			String groupName = nacosContextProperties.getGroup();
			if (StringUtils.isNotBlank(groupName) && !DEFAULT_GROUP.equals(groupName)) {
				instanceMetadata.put(NACOS_GROUP_KEY, groupName);
			}
		}
	}
}
