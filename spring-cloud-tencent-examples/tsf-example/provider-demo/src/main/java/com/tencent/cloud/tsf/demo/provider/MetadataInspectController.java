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

package com.tencent.cloud.tsf.demo.provider;

import java.util.LinkedHashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dump incoming TSF/SCT headers and MetadataContext buckets for tag verification.
 */
@RestController
public class MetadataInspectController {

	private static final Logger LOG = LoggerFactory.getLogger(MetadataInspectController.class);

	@GetMapping("/metadata")
	public Map<String, Object> metadata(HttpServletRequest request) {
		MetadataContext ctx = MetadataContextHolder.get();
		Map<String, Object> body = new LinkedHashMap<>();
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put(MetadataConstant.HeaderName.TSF_TAGS,
				request.getHeader(MetadataConstant.HeaderName.TSF_TAGS));
		headers.put(MetadataConstant.HeaderName.TSF_SYSTEM_TAG,
				request.getHeader(MetadataConstant.HeaderName.TSF_SYSTEM_TAG));
		headers.put(MetadataConstant.HeaderName.TSF_METADATA,
				request.getHeader(MetadataConstant.HeaderName.TSF_METADATA));
		headers.put(MetadataConstant.HeaderName.CUSTOM_METADATA,
				request.getHeader(MetadataConstant.HeaderName.CUSTOM_METADATA));
		headers.put(MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA,
				request.getHeader(MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA));
		body.put("headers", headers);
		body.put("calleeTransitive", ctx.getTransitiveMetadata());
		body.put("calleeDisposable", ctx.getDisposableMetadata());
		body.put("callerDisposable", ctx.getFragmentContext(MetadataContext.FRAGMENT_UPSTREAM_DISPOSABLE));
		LOG.info("GET /metadata incoming headers TSF-Tags={} TSF-System-Tags={} TSF-Metadata={} "
						+ "SCT-CUSTOM-METADATA={} SCT-CUSTOM-DISPOSABLE-METADATA={}",
				headers.get(MetadataConstant.HeaderName.TSF_TAGS),
				headers.get(MetadataConstant.HeaderName.TSF_SYSTEM_TAG),
				headers.get(MetadataConstant.HeaderName.TSF_METADATA),
				headers.get(MetadataConstant.HeaderName.CUSTOM_METADATA),
				headers.get(MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA));
		LOG.info("GET /metadata MetadataContext calleeTransitive={} calleeDisposable={} callerDisposable={}",
				body.get("calleeTransitive"), body.get("calleeDisposable"), body.get("callerDisposable"));
		return body;
	}
}
