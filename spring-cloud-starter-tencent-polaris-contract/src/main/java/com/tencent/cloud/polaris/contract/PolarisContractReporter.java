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
import com.tencent.cloud.polaris.contract.config.PolarisContractProperties;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.plugin.server.InterfaceDescriptor;
import com.tencent.polaris.api.plugin.server.ReportServiceContractRequest;
import com.tencent.polaris.api.plugin.server.ReportServiceContractResponse;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.AbstractOpenApiResource;
import org.springdoc.api.AbstractOpenApiResourceUtil;
import org.springdoc.webflux.api.OpenApiWebFluxUtil;
import org.springdoc.webmvc.api.OpenApiWebMvcUtil;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

/**
 * Polaris contract reporter.
 *
 * @author Haotian Zhang
 */
public class PolarisContractReporter implements ApplicationListener<ApplicationReadyEvent> {

	private final Logger LOG = LoggerFactory.getLogger(PolarisContractReporter.class);

	private final org.springdoc.webmvc.api.MultipleOpenApiResource multipleOpenApiWebMvcResource;
	private final org.springdoc.webflux.api.MultipleOpenApiResource multipleOpenApiWebFluxResource;
	private final PolarisContractProperties polarisContractProperties;

	private final ProviderAPI providerAPI;

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	public PolarisContractReporter(org.springdoc.webmvc.api.MultipleOpenApiResource multipleOpenApiWebMvcResource,
			org.springdoc.webflux.api.MultipleOpenApiResource multipleOpenApiWebFluxResource,
			PolarisContractProperties polarisContractProperties, ProviderAPI providerAPI,
			PolarisDiscoveryProperties polarisDiscoveryProperties) {
		this.multipleOpenApiWebMvcResource = multipleOpenApiWebMvcResource;
		this.multipleOpenApiWebFluxResource = multipleOpenApiWebFluxResource;
		this.polarisContractProperties = polarisContractProperties;
		this.providerAPI = providerAPI;
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
	}

	@Override
	public void onApplicationEvent(@NonNull ApplicationReadyEvent applicationReadyEvent) {
		if (polarisContractProperties.isReportEnabled()) {
			try {
				AbstractOpenApiResource openApiResource = null;
				if (multipleOpenApiWebMvcResource != null) {
					openApiResource = OpenApiWebMvcUtil.getOpenApiResourceOrThrow(multipleOpenApiWebMvcResource, polarisContractProperties.getGroup());
				}
				else if (multipleOpenApiWebFluxResource != null) {
					openApiResource = OpenApiWebFluxUtil.getOpenApiResourceOrThrow(multipleOpenApiWebFluxResource, polarisContractProperties.getGroup());
				}
				OpenAPI openAPI = null;
				if (openApiResource != null) {
					openAPI = AbstractOpenApiResourceUtil.getOpenApi(openApiResource);
				}
				if (openAPI != null) {
					ReportServiceContractRequest request = new ReportServiceContractRequest();
					request.setName(polarisDiscoveryProperties.getService());
					request.setNamespace(polarisDiscoveryProperties.getNamespace());
					request.setService(polarisDiscoveryProperties.getService());
					request.setProtocol("http");
					request.setVersion(polarisDiscoveryProperties.getVersion());
					List<InterfaceDescriptor> interfaceDescriptorList = getInterfaceDescriptorFromSwagger(openAPI);
					request.setInterfaceDescriptors(interfaceDescriptorList);
					ReportServiceContractResponse response = providerAPI.reportServiceContract(request);
					LOG.info("Service contract [Namespace: {}. Name: {}. Service: {}. Protocol:{}. Version: {}. API counter: {}] is reported.",
							request.getNamespace(), request.getName(), request.getService(), request.getProtocol(),
							request.getVersion(), request.getInterfaceDescriptors().size());
					if (LOG.isDebugEnabled()) {
						String jsonValue = JacksonUtils.serialize2Json(openAPI);
						LOG.debug("OpenApi json data: {}", jsonValue);
					}
				}
				else {
					LOG.warn("OpenAPI or json is null, group:{}", polarisContractProperties.getGroup());
				}
			}
			catch (Throwable t) {
				LOG.error("Report contract failed.", t);
			}
		}
	}

	private List<InterfaceDescriptor> getInterfaceDescriptorFromSwagger(OpenAPI openAPI) {
		List<InterfaceDescriptor> interfaceDescriptorList = new ArrayList<>();
		Paths paths = openAPI.getPaths();
		for (Map.Entry<String, PathItem> p : paths.entrySet()) {
			PathItem path = p.getValue();
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

	private Map<String, Operation> getOperationMapFromPath(PathItem path) {
		Map<String, Operation> operationMap = new HashMap<>();

		if (path.getGet() != null) {
			operationMap.put(PathItem.HttpMethod.GET.name(), path.getGet());
		}
		if (path.getPut() != null) {
			operationMap.put(PathItem.HttpMethod.PUT.name(), path.getPut());
		}
		if (path.getPost() != null) {
			operationMap.put(PathItem.HttpMethod.POST.name(), path.getPost());
		}
		if (path.getHead() != null) {
			operationMap.put(PathItem.HttpMethod.HEAD.name(), path.getHead());
		}
		if (path.getDelete() != null) {
			operationMap.put(PathItem.HttpMethod.DELETE.name(), path.getDelete());
		}
		if (path.getPatch() != null) {
			operationMap.put(PathItem.HttpMethod.PATCH.name(), path.getPatch());
		}
		if (path.getOptions() != null) {
			operationMap.put(PathItem.HttpMethod.OPTIONS.name(), path.getOptions());
		}

		return operationMap;
	}
}
