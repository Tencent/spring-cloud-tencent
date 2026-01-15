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

package com.tencent.cloud.polaris.router;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.rpc.enhancement.transformer.InstanceTransformer;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.CollectionUtils;

/**
 * load balancer utils.
 *
 * @author lepdou 2022-05-17
 */
public final class RouterUtils {

	private RouterUtils() {
	}

	/**
	 * transfer servers to ServiceInstances.
	 *
	 * @param servers servers
	 * @return ServiceInstances
	 */
	public static Mono<ServiceInstances> transferServersToServiceInstances(
			Flux<List<ServiceInstance>> servers, InstanceTransformer instanceTransformer) {
		return servers.flatMapIterable(Function.identity())
				.map(instanceTransformer::transform)
				.collectList()
				.map(instanceList -> {
					String serviceName = "";
					Map<String, String> serviceMetadata = new HashMap<>();
					if (!CollectionUtils.isEmpty(instanceList)) {
						serviceName = instanceList.get(0).getService();
						serviceMetadata = instanceList.get(0).getServiceMetadata();
					}

					String namespace = MetadataContextHolder.get()
							.getContextWithDefault(MetadataContext.FRAGMENT_APPLICATION_NONE,
									MetadataConstant.POLARIS_TARGET_NAMESPACE, MetadataContext.LOCAL_NAMESPACE);

					ServiceKey serviceKey = new ServiceKey(namespace, serviceName);

					return new DefaultServiceInstances(serviceKey, instanceList, serviceMetadata);
				});
	}
}
