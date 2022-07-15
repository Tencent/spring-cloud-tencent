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

package com.tencent.cloud.metadata.service.middle;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static com.tencent.cloud.common.util.JacksonUtils.serialize2Json;

/**
 * Metadata callee controller.
 *
 * @author Palmer Xu
 */
@RestController
@RequestMapping("/metadata/service/middle")
public class MetadataMiddleController {

	private static final Logger LOG = LoggerFactory.getLogger(MetadataMiddleController.class);

	@Value("${server.port:0}")
	private int port;

	private final MetadataBackendService metadataBackendService;

	private final RestTemplate restTemplate;

	public MetadataMiddleController(MetadataBackendService metadataBackendService, RestTemplate restTemplate) {
		this.metadataBackendService = metadataBackendService;
		this.restTemplate = restTemplate;
	}

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/info")
	public Map<String, Map<String, String>> info() {
		LOG.info("Metadata Middle Service [{}] is called.", port);

		// Call remote service with RestTemplate
		Map<String, Map<String, String>> backendResult = restTemplate.getForObject(
				"http://MetadataBackendService/metadata/service/backend/info", Map.class);

		if (backendResult != null) {
			LOG.info("RestTemplate Backend Metadata");
			serialize2Json(backendResult, true);
			backendResult.clear();
		}

		// Call remote service with Feign
		backendResult = metadataBackendService.info();
		if (backendResult != null) {
			LOG.info("Feign Backend Metadata");
			serialize2Json(backendResult, true);

			backendResult.clear();
		}

		// Build result
		Map<String, Map<String, String>> ret = new HashMap<>();

		// Get Custom Metadata From Context
		MetadataContext context = MetadataContextHolder.get();
		Map<String, String> customMetadataMap = context.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

		customMetadataMap.forEach((key, value) -> {
			LOG.info("Metadata Backend Custom Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("transitive-metadata", customMetadataMap);

		// Get Disposable metadata from upstream service
		Optional<String> upstreamDisposableMetadata = MetadataContextHolder.getDisposableMetadata("", true);
		LOG.info("Upstream Disposable Metadata (Key-Value) : {}, {}", "", upstreamDisposableMetadata.get());

		// Get All Disposable metadata from upstream service
		Map<String, String> upstreamDisposableMetadatas = MetadataContextHolder.getAllDisposableMetadata(true);
		upstreamDisposableMetadatas.forEach((key, value) -> {
			LOG.info("Backend All Upstream Custom Disposable Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("upstream-disposable-metadata", upstreamDisposableMetadatas);

		// Get All Disposable metadata from upstream service
		Map<String, String> localDisposableMetadatas = MetadataContextHolder.getAllDisposableMetadata(false);
		localDisposableMetadatas.forEach((key, value) -> {
			LOG.info("Backend All Upstream Custom Disposable Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("local-disposable-metadata", localDisposableMetadatas);

		return ret;
	}

}
