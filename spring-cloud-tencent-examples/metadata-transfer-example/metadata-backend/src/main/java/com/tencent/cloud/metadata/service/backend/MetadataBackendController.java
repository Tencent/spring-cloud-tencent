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

package com.tencent.cloud.metadata.service.backend;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Metadata callee controller.
 *
 * @author Palmer Xu
 */
@RestController
@RequestMapping("/metadata/service/backend")
public class MetadataBackendController {

	private static final Logger LOG = LoggerFactory.getLogger(MetadataBackendController.class);

	@Value("${server.port:0}")
	private int port;

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/info")
	public Map<String, Map<String, String>> info() {
		LOG.info("Metadata Backend Service [{}] is called.", port);
		Map<String, Map<String, String>> ret = new HashMap<>();

		// Get Custom Metadata From Context
		MetadataContext context = MetadataContextHolder.get();
		Map<String, String> customMetadataMap = context.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

		customMetadataMap.forEach((key, value) -> {
			LOG.info("Metadata Backend Custom Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("backend-transitive-metadata", customMetadataMap);

		// Get All Disposable metadata from upstream service
		Map<String, String> upstreamDisposableMetadatas = MetadataContextHolder.getAllDisposableMetadata(true);
		upstreamDisposableMetadatas.forEach((key, value) -> {
			LOG.info("Upstream Disposable Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("backend-upstream-disposable-metadata", upstreamDisposableMetadatas);

		// Get All Disposable metadata from upstream service
		Map<String, String> localDisposableMetadatas = MetadataContextHolder.getAllDisposableMetadata(false);
		localDisposableMetadatas.forEach((key, value) -> {
			LOG.info("Local Custom Disposable Metadata (Key-Value): {} : {}", key, value);
		});

		ret.put("backend-local-disposable-metadata", localDisposableMetadatas);

		return ret;
	}
}
