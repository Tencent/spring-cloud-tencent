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

package com.tencent.cloud.tsf.demo.consumer.controller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.tsf.demo.consumer.proxy.ProviderDemoService;
import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.tsf.core.TsfContext;
import org.springframework.tsf.core.entity.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Put TSF tags then dump local context or call provider /metadata.
 */
@RestController
public class MetadataInspectController {

	private static final Logger LOG = LoggerFactory.getLogger(MetadataInspectController.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ProviderDemoService providerDemoService;

	@GetMapping("/local-metadata")
	public Map<String, Object> localMetadata(
			@RequestParam(required = false) String hopKey,
			@RequestParam(required = false) String hopValue,
			@RequestParam(required = false) String passKey,
			@RequestParam(required = false) String passValue) {
		LOG.info("GET /local-metadata hop={}:{} pass={}:{}", hopKey, hopValue, passKey, passValue);
		putTags(hopKey, hopValue, passKey, passValue);
		Map<String, Object> body = dumpLocal();
		LOG.info("GET /local-metadata dump after putTag (no outbound): {}", body);
		return body;
	}

	@GetMapping("/echo-metadata-rest")
	@SuppressWarnings("unchecked")
	public Map<String, Object> echoMetadataRest(
			@RequestParam(required = false) String hopKey,
			@RequestParam(required = false) String hopValue,
			@RequestParam(required = false) String passKey,
			@RequestParam(required = false) String passValue) {
		LOG.info("GET /echo-metadata-rest hop={}:{} pass={}:{}, RestTemplate -> provider-demo /metadata",
				hopKey, hopValue, passKey, passValue);
		putTags(hopKey, hopValue, passKey, passValue);
		Map<String, Object> body = restTemplate.getForObject("http://provider-demo/metadata", Map.class);
		LOG.info("GET /echo-metadata-rest provider /metadata response: {}", body);
		return body;
	}

	@GetMapping("/echo-metadata-feign")
	@SuppressWarnings("unchecked")
	public Map<String, Object> echoMetadataFeign(
			@RequestParam(required = false) String hopKey,
			@RequestParam(required = false) String hopValue,
			@RequestParam(required = false) String passKey,
			@RequestParam(required = false) String passValue) {
		LOG.info("GET /echo-metadata-feign hop={}:{} pass={}:{}, "
						+ "Feign ProviderDemoService#metadata() -> provider-demo /metadata",
				hopKey, hopValue, passKey, passValue);
		putTags(hopKey, hopValue, passKey, passValue);
		Map<String, Object> body = providerDemoService.metadata();
		LOG.info("GET /echo-metadata-feign provider /metadata response: {}", body);
		return body;
	}

	private void putTags(String hopKey, String hopValue, String passKey, String passValue) {
		if (StringUtils.isNotBlank(hopKey)) {
			TsfContext.putTag(hopKey, hopValue);
			LOG.info("TsfContext.putTag one-hop (no flag) {}:{}", hopKey, hopValue);
		}
		if (StringUtils.isNotBlank(passKey)) {
			TsfContext.putTags(Collections.singletonMap(passKey, passValue), Tag.ControlFlag.TRANSITIVE);
			LOG.info("TsfContext.putTags TRANSITIVE {}:{}", passKey, passValue);
		}
	}

	private Map<String, Object> dumpLocal() {
		MetadataContext ctx = MetadataContextHolder.get();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("calleeTransitive", ctx.getTransitiveMetadata());
		body.put("calleeDisposable", ctx.getDisposableMetadata());
		body.put("callerDisposable", ctx.getFragmentContext(MetadataContext.FRAGMENT_UPSTREAM_DISPOSABLE));
		return body;
	}
}
