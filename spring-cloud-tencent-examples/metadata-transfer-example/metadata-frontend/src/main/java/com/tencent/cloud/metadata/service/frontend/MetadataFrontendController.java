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

package com.tencent.cloud.metadata.service.frontend;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@RequestMapping("/metadata/service/frontend")
public class MetadataFrontendController {

	private static final Logger LOG = LoggerFactory.getLogger(MetadataFrontendController.class);

	private final RestTemplate restTemplate;

	private final MetadataMiddleService metadataMiddleService;

	public MetadataFrontendController(RestTemplate restTemplate,
			MetadataMiddleService metadataMiddleService) {
		this.restTemplate = restTemplate;
		this.metadataMiddleService = metadataMiddleService;
	}

	/**
	 * Get metadata info from remote service.
	 *
	 * @return metadata map
	 */
	@GetMapping("/feign/info")
	public Map<String, Map<String, String>> feign() {
		Map<String, Map<String, String>> ret = new HashMap<>();

		// Call remote service with feign client
		Map<String, Map<String, String>> middleResult = metadataMiddleService.info();

		if (middleResult != null) {
			ret.putAll(middleResult);
		}

		// Get Custom Metadata From Context
		MetadataContext context = MetadataContextHolder.get();
		Map<String, String> customMetadataMap = context.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

		customMetadataMap.forEach((key, value) -> {
			LOG.info("Metadata Middle Custom Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("frontend-transitive-metadata", customMetadataMap);

		// Get All Disposable metadata from upstream service
		Map<String, String> upstreamDisposableMetadatas = MetadataContextHolder.getAllDisposableMetadata(true);
		upstreamDisposableMetadatas.forEach((key, value) -> {
			LOG.info("Upstream Custom Disposable Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("frontend-upstream-disposable-metadata", upstreamDisposableMetadatas);

		// Get All Disposable metadata from upstream service
		Map<String, String> localDisposableMetadatas = MetadataContextHolder.getAllDisposableMetadata(false);
		localDisposableMetadatas.forEach((key, value) -> {
			LOG.info("Local Custom Disposable Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("frontend-local-disposable-metadata", localDisposableMetadatas);

		return ret;
	}

	/**
	 * Get metadata information of callee.
	 *
	 * @return information of callee
	 */
	@SuppressWarnings("unchecked")
	@GetMapping("/rest/info")
	public Map<String, Map<String, String>> rest() {
		Map<String, Map<String, String>> ret = new HashMap<>();

		// Call remote service with RestTemplate
		Map<String, Map<String, String>> middleResult = restTemplate.getForObject(
				"http://MetadataMiddleService/metadata/service/middle/info", Map.class);

		if (middleResult != null) {
			ret.putAll(middleResult);
		}

		// Get Custom Metadata From Context
		MetadataContext context = MetadataContextHolder.get();
		Map<String, String> customMetadataMap = context.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

		customMetadataMap.forEach((key, value) -> {
			LOG.info("Metadata Middle Custom Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("frontend-transitive-metadata", customMetadataMap);

		// Get All Disposable metadata from upstream service
		Map<String, String> upstreamDisposableMetadatas = MetadataContextHolder.getAllDisposableMetadata(true);
		upstreamDisposableMetadatas.forEach((key, value) -> {
			LOG.info("Upstream Custom Disposable Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("frontend-upstream-disposable-metadata", upstreamDisposableMetadatas);

		// Get All Disposable metadata from upstream service
		Map<String, String> localDisposableMetadatas = MetadataContextHolder.getAllDisposableMetadata(false);
		localDisposableMetadatas.forEach((key, value) -> {
			LOG.info("Local Custom Disposable Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("frontend-local-disposable-metadata", localDisposableMetadatas);

		return ret;
	}

	/**
	 * health check.
	 *
	 * @return health check info
	 */
	@GetMapping("/healthCheck")
	public String healthCheck() {
		return "pk ok";
	}
}
