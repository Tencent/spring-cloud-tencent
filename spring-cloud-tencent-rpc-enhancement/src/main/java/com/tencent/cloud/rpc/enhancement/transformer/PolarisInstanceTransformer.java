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

package com.tencent.cloud.rpc.enhancement.transformer;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.pojo.PolarisServiceInstance;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.utils.CollectionUtils;

import org.springframework.cloud.client.ServiceInstance;

/**
 * PolarisInstanceTransformer.
 *
 * @author sean yu
 */
public class PolarisInstanceTransformer implements InstanceTransformer {

	@Override
	public void transformCustom(DefaultInstance instance, ServiceInstance serviceInstance) {
		if (serviceInstance instanceof PolarisServiceInstance) {
			PolarisServiceInstance polarisServiceInstance = (PolarisServiceInstance) serviceInstance;
			instance.setRegion(polarisServiceInstance.getPolarisInstance().getRegion());
			instance.setZone(polarisServiceInstance.getPolarisInstance().getZone());
			instance.setCampus(polarisServiceInstance.getPolarisInstance().getCampus());
			instance.setWeight(polarisServiceInstance.getPolarisInstance().getWeight());
			instance.setCreateTime(polarisServiceInstance.getPolarisInstance().getCreateTime());
			if (CollectionUtils.isNotEmpty(polarisServiceInstance.getServiceMetadata())) {
				instance.setServiceMetadata(polarisServiceInstance.getServiceMetadata());
			}

			String namespace = MetadataContextHolder.get().getContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
					MetadataConstant.POLARIS_TARGET_NAMESPACE, instance.getNamespace());

			instance.setNamespace(namespace);
		}
	}

}
