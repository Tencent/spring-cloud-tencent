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

package com.tencent.cloud.rpc.enhancement.transformer;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.Instance;

import org.springframework.cloud.client.ServiceInstance;

/**
 * InstanceTransformer.
 *
 * @author sean yu
 */
public interface InstanceTransformer {

	default Instance transform(ServiceInstance serviceInstance) {
		DefaultInstance instance = new DefaultInstance();
		transformDefault(instance, serviceInstance);
		transformCustom(instance, serviceInstance);
		return instance;
	}

	default void transformDefault(DefaultInstance instance, ServiceInstance serviceInstance) {
		instance.setNamespace(MetadataContext.LOCAL_NAMESPACE);
		instance.setService(serviceInstance.getServiceId());
		instance.setProtocol(serviceInstance.getScheme());
		instance.setId(serviceInstance.getInstanceId());
		instance.setHost(serviceInstance.getHost());
		instance.setPort(serviceInstance.getPort());
		instance.setMetadata(serviceInstance.getMetadata());
	}

	void transformCustom(DefaultInstance instance, ServiceInstance serviceInstance);

}
