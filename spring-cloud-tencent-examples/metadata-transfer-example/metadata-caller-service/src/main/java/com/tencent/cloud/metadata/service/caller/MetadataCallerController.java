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

package com.tencent.cloud.metadata.service.caller;

import java.util.Map;

import com.google.common.collect.Maps;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Metadata caller controller.
 *
 * @author Palmer Xu
 */
@RestController
@RequestMapping("/metadata/service/caller")
public class MetadataCallerController {

	private final RestTemplate restTemplate;

	private final MetadataCalleeService metadataCalleeService;

	private final MetadataLocalProperties metadataLocalProperties;

	public MetadataCallerController(RestTemplate restTemplate,
			MetadataCalleeService metadataCalleeService,
			MetadataLocalProperties metadataLocalProperties) {
		this.restTemplate = restTemplate;
		this.metadataCalleeService = metadataCalleeService;
		this.metadataLocalProperties = metadataLocalProperties;
	}

	/**
	 * Get metadata info from remote service.
	 * @return metadata map
	 */
	@GetMapping("/feign/info")
	public Map<String, Map<String, String>> feign() {
		Map<String, Map<String, String>> ret = Maps.newHashMap();

		// Call remote service with feign client
		Map<String, String> calleeMetadata = metadataCalleeService.info();
		ret.put("callee-transitive-metadata", calleeMetadata);

		// Get Custom Metadata From Context
		MetadataContext context = MetadataContextHolder.get();
		Map<String, String> callerTransitiveMetadata = context.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		ret.put("caller-transitive-metadata", callerTransitiveMetadata);
		ret.put("caller-metadata-contents", metadataLocalProperties.getContent());

		return ret;
	}

	/**
	 * Get metadata information of callee.
	 * @return information of callee
	 */
	@SuppressWarnings("unchecked")
	@GetMapping("/rest/info")
	public Map<String, Map<String, String>> rest() {
		Map<String, Map<String, String>> ret = Maps.newHashMap();

		// Call remote service with RestTemplate
		Map<String, String> calleeMetadata = restTemplate.getForObject(
				"http://MetadataCalleeService/metadata/service/callee/info",
				Map.class);
		ret.put("callee-transitive-metadata", calleeMetadata);

		// Get Custom Metadata From Context
		MetadataContext context = MetadataContextHolder.get();
		Map<String, String> callerTransitiveMetadata = context.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		ret.put("caller-transitive-metadata", callerTransitiveMetadata);
		ret.put("caller-metadata-contents", metadataLocalProperties.getContent());

		return ret;
	}

	/**
	 * health check.
	 * @return health check info
	 */
	@GetMapping("/healthCheck")
	public String healthCheck() {
		return "pk ok";
	}

}
