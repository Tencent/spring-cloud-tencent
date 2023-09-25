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

package com.tencent.cloud.polaris.contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.plugin.server.InterfaceDescriptor;
import com.tencent.polaris.api.plugin.server.ReportServiceContractRequest;
import com.tencent.polaris.api.plugin.server.ReportServiceContractResponse;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import springfox.documentation.service.Documentation;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.spring.web.json.JsonSerializer;
import springfox.documentation.swagger2.mappers.ServiceModelToSwagger2Mapper;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

public class PolarisContractReporter implements ApplicationListener<ApplicationReadyEvent> {

	private final Logger LOG = LoggerFactory.getLogger(PolarisContractReporter.class);
	private final ServiceModelToSwagger2Mapper swagger2Mapper;
	private final DocumentationCache documentationCache;
	private final JsonSerializer jsonSerializer;
	private final String groupName;

	private final ProviderAPI providerAPI;

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	public PolarisContractReporter(DocumentationCache documentationCache, ServiceModelToSwagger2Mapper swagger2Mapper,
			JsonSerializer jsonSerializer, String groupName, ProviderAPI providerAPI,
			PolarisDiscoveryProperties polarisDiscoveryProperties) {
		this.swagger2Mapper = swagger2Mapper;
		this.documentationCache = documentationCache;
		this.jsonSerializer = jsonSerializer;
		this.groupName = groupName;
		this.providerAPI = providerAPI;
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
	}

	@Override
	public void onApplicationEvent(@NonNull ApplicationReadyEvent applicationReadyEvent) {
		try {
			Documentation documentation = documentationCache.documentationByGroup(groupName);
			Swagger swagger = swagger2Mapper.mapDocumentation(documentation);
			if (swagger != null) {
				ReportServiceContractRequest request = new ReportServiceContractRequest();
				request.setName(polarisDiscoveryProperties.getService());
				request.setNamespace(polarisDiscoveryProperties.getNamespace());
				request.setService(polarisDiscoveryProperties.getService());
				request.setProtocol("http");
				request.setVersion(polarisDiscoveryProperties.getVersion());
				List<InterfaceDescriptor> interfaceDescriptorList = getInterfaceDescriptorFromSwagger(swagger);
				request.setInterfaceDescriptors(interfaceDescriptorList);
				ReportServiceContractResponse response = providerAPI.reportServiceContract(request);
				LOG.info("Service contract [Namespace: {}. Name: {}. Service: {}. Protocol:{}. Version: {}. API counter: {}] is reported.",
						request.getNamespace(), request.getName(), request.getService(), request.getProtocol(),
						request.getVersion(), request.getInterfaceDescriptors().size());
				if (LOG.isDebugEnabled()) {
					String jsonValue = JacksonUtils.serialize2Json(swagger);
					LOG.debug("OpenApi json data: {}", jsonValue);
				}
			}
			else {
				LOG.warn("Swagger or json is null, documentationCache keys:{}, group:{}", documentationCache.all()
						.keySet(), groupName);
			}
		}
		catch (Throwable t) {
			LOG.error("Report contract failed.", t);
		}
	}

	private List<InterfaceDescriptor> getInterfaceDescriptorFromSwagger(Swagger swagger) {
		List<InterfaceDescriptor> interfaceDescriptorList = new ArrayList<>();
		Map<String, Path> paths = swagger.getPaths();
		for (Map.Entry<String, Path> p : paths.entrySet()) {
			Path path = p.getValue();
			Map<String, Operation> operationMap = getOperationMapFromPath(path);
			if (CollectionUtils.isEmpty(operationMap)) {
				continue;
			}
			for (Map.Entry<String, Operation> o : operationMap.entrySet()) {
				InterfaceDescriptor interfaceDescriptor = new InterfaceDescriptor();
				interfaceDescriptor.setPath(p.getKey());
				interfaceDescriptor.setMethod(o.getKey());
				interfaceDescriptor.setContent(JacksonUtils.serialize2Json(p.getValue()));
				interfaceDescriptorList.add(interfaceDescriptor);
			}
		}
		return interfaceDescriptorList;
	}

	private Map<String, Operation> getOperationMapFromPath(Path path) {
		Map<String, Operation> operationMap = new HashMap<>();

		if (path.getGet() != null) {
			operationMap.put(HttpMethod.GET.name(), path.getGet());
		}
		if (path.getPut() != null) {
			operationMap.put(HttpMethod.PUT.name(), path.getPut());
		}
		if (path.getPost() != null) {
			operationMap.put(HttpMethod.POST.name(), path.getPost());
		}
		if (path.getHead() != null) {
			operationMap.put(HttpMethod.HEAD.name(), path.getHead());
		}
		if (path.getDelete() != null) {
			operationMap.put(HttpMethod.DELETE.name(), path.getDelete());
		}
		if (path.getPatch() != null) {
			operationMap.put(HttpMethod.PATCH.name(), path.getPatch());
		}
		if (path.getOptions() != null) {
			operationMap.put(HttpMethod.OPTIONS.name(), path.getOptions());
		}

		return operationMap;
	}
}
