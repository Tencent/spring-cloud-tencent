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
 *
 */

package com.tencent.cloud.plugin.discovery.adapter.transformer;

import com.tencent.cloud.rpc.enhancement.transformer.InstanceTransformer;
import com.tencent.polaris.api.pojo.DefaultInstance;
import org.apache.commons.lang.StringUtils;

import org.springframework.cloud.client.ServiceInstance;

/**
 * NacosInstanceTransformer.
 *
 * @author sean yu
 */
public class NacosInstanceTransformer implements InstanceTransformer {

	@Override
	public void transformCustom(DefaultInstance instance, ServiceInstance serviceInstance) {
		if ("com.alibaba.cloud.nacos.NacosServiceInstance".equals(serviceInstance.getClass().getName())) {
			String nacosWeight = serviceInstance.getMetadata().get("nacos.weight");
			instance.setWeight(
					StringUtils.isBlank(nacosWeight) ? 100 : (int) Double.parseDouble(nacosWeight) * 100
			);
			String nacosHealthy = serviceInstance.getMetadata().get("nacos.healthy");
			instance.setHealthy(
					!StringUtils.isBlank(nacosHealthy) && Boolean.parseBoolean(nacosHealthy)
			);
			String nacosInstanceId = serviceInstance.getMetadata().get("nacos.instanceId");
			instance.setId(nacosInstanceId);
		}
	}

}
