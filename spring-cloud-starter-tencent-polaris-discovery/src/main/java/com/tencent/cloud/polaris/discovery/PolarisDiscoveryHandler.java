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

package com.tencent.cloud.polaris.discovery;

import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant.SystemMetadataKey;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.polaris.PolarisProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.GetServicesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServicesResponse;
import org.apache.commons.lang.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Discovery Handler for Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
@Component
public class PolarisDiscoveryHandler {

	@Autowired
	private PolarisProperties polarisProperties;

	@Autowired
	private ProviderAPI providerAPI;

	@Autowired
	private ConsumerAPI polarisConsumer;

	/**
	 * Get a list of instances after service routing.
	 * @param service service name
	 * @return list of instances
	 */
	public InstancesResponse getFilteredInstances(String service) {
		String namespace = polarisProperties.getNamespace();
		GetInstancesRequest getInstancesRequest = new GetInstancesRequest();
		getInstancesRequest.setNamespace(namespace);
		getInstancesRequest.setService(service);
		String method = MetadataContextHolder.get()
				.getSystemMetadata(SystemMetadataKey.PEER_PATH);
		getInstancesRequest.setMethod(method);
		String localNamespace = MetadataContext.LOCAL_NAMESPACE;
		String localService = MetadataContext.LOCAL_SERVICE;
		Map<String, String> allTransitiveCustomMetadata = MetadataContextHolder.get()
				.getAllTransitiveCustomMetadata();
		if (StringUtils.isNotBlank(localNamespace) || StringUtils.isNotBlank(localService)
				|| null != allTransitiveCustomMetadata) {
			ServiceInfo sourceService = new ServiceInfo();
			sourceService.setNamespace(localNamespace);
			sourceService.setService(localService);
			sourceService.setMetadata(allTransitiveCustomMetadata);
			getInstancesRequest.setServiceInfo(sourceService);
		}
		return polarisConsumer.getInstances(getInstancesRequest);
	}

	/**
	 * Return all instances for the given service.
	 * @param service serviceName
	 * @return list of instances
	 */
	public InstancesResponse getInstances(String service) {
		String namespace = polarisProperties.getNamespace();
		GetAllInstancesRequest request = new GetAllInstancesRequest();
		request.setNamespace(namespace);
		request.setService(service);
		return polarisConsumer.getAllInstance(request);
	}

	public ProviderAPI getProviderAPI() {
		return providerAPI;
	}

	/**
	 * Return all service for given namespace.
	 * @return service list
	 */
	public ServicesResponse GetServices() {
		String namespace = polarisProperties.getNamespace();
		GetServicesRequest request = new GetServicesRequest();
		request.setNamespace(namespace);
		return polarisConsumer.getServices(request);
	}

}
