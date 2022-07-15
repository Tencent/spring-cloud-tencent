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

package com.tencent.cloud.metadata.service.callee;

import java.util.Map;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Metadata callee controller.
 *
 * @author Palmer Xu
 */
@RestController
@RequestMapping("/metadata/service/callee")
public class MetadataCalleeController {

	private static final Logger LOG = LoggerFactory.getLogger(MetadataCalleeController.class);

	@Value("${server.port:0}")
	private int port;

	private final MetadataBackendService metadataBackendService;

	private final RestTemplate restTemplate;

	public MetadataCalleeController(MetadataBackendService metadataBackendService, RestTemplate restTemplate) {
		this.metadataBackendService = metadataBackendService;
		this.restTemplate = restTemplate;
	}

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/info")
	public Map<String, String> info() {
		LOG.info("Metadata Service Callee [{}] is called.", port);

		// Call remote service with RestTemplate
		Map<String, String> calleeMetadata = restTemplate.getForObject(
				"http://MetadataCalleeService2/metadata/service/callee2/info",
				Map.class);
		calleeMetadata.forEach((key, value) -> {
			LOG.info("RestTemplate Callee2 Metadata (Key-Value): {} : {}", key, value);
		});

		// Call remote service with Feign
		Map<String, String> calleeMetadata2 = metadataBackendService.info();
		calleeMetadata2.forEach((key, value) -> {
			LOG.info("Feign Callee2 Metadata (Key-Value): {} : {}", key, value);
		});

		// Get Custom Metadata From Context
		MetadataContext context = MetadataContextHolder.get();
		Map<String, String> customMetadataMap = context.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

		customMetadataMap.forEach((key, value) -> {
			LOG.info("Custom Metadata (Key-Value): {} : {}", key, value);
		});

		return customMetadataMap;
	}

}
