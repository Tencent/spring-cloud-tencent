/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.tencent.cloud.common.util.PolarisCompletableFutureUtils;
import com.tencent.cloud.tsf.demo.consumer.proxy.ProviderDemoService;
import com.tencent.cloud.tsf.demo.consumer.proxy.ProviderService;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.tsf.core.TsfContext;
import org.springframework.tsf.core.entity.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ConsumerController {
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private ProviderService providerService;
	@Autowired
	private ProviderDemoService providerDemoService;

	@RequestMapping(value = "/echo-rest/{str}", method = RequestMethod.GET)
	public String restProvider(@PathVariable String str,
			@RequestParam(required = false) String tagName,
			@RequestParam(required = false) String tagValue) {
		if (StringUtils.isNotBlank(tagName)) {
			TsfContext.putTag(tagName, tagValue);
		}
		TsfContext.putTag("operation", "rest");
		Map<String, String> mTags = new HashMap<>();
		mTags.put("rest-trace-key1", "value1");
		mTags.put("rest-trace-key2", "value2");
		TsfContext.putTags(mTags, Tag.ControlFlag.TRANSITIVE);
		try {
			return restTemplate.getForObject("http://provider-demo/echo/" + str, String.class);
		}
		catch (CallAbortedException callAbortedException) {
			return callAbortedException.getMessage();
		}
	}


	@RequestMapping(value = "/echo-rest-async/{str}", method = RequestMethod.GET)
	public String restAsync(@PathVariable String str,
			@RequestParam(required = false) String tagName,
			@RequestParam(required = false) String tagValue) throws ExecutionException, InterruptedException {
		if (StringUtils.isNotBlank(tagName)) {
			TsfContext.putTag(tagName, tagValue);
		}
		TsfContext.putTag("operation", "rest");
		Map<String, String> mTags = new HashMap<>();
		mTags.put("rest-trace-key1", "value1");
		mTags.put("rest-trace-key2", "value2");
		TsfContext.putTags(mTags, Tag.ControlFlag.TRANSITIVE);
		CompletableFuture<String> echoFuture = PolarisCompletableFutureUtils.supplyAsync(() -> {
			try {
				return restTemplate.getForObject("http://provider-demo/echo/" + str, String.class);
			}
			catch (CallAbortedException callAbortedException) {
				return callAbortedException.getMessage();
			}
		});
		return echoFuture.get();
	}

	@RequestMapping(value = "/echo-feign/{str}", method = RequestMethod.GET)
	public String feignProvider(@PathVariable String str,
			@RequestParam(required = false) String tagName,
			@RequestParam(required = false) String tagValue) {
		if (StringUtils.isNotBlank(tagName)) {
			TsfContext.putTag(tagName, tagValue);
		}
		TsfContext.putTag("operation", "feign");
		Map<String, String> mTags = new HashMap<>();
		mTags.put("feign-trace-key1", "value1");
		mTags.put("feign-trace-key2", "value2");
		TsfContext.putTags(mTags, Tag.ControlFlag.TRANSITIVE);
		try {
			return providerDemoService.echo(str);
		}
		catch (CallAbortedException callAbortedException) {
			return callAbortedException.getMessage();
		}
	}

	@RequestMapping(value = "/echo-feign-url/{str}", method = RequestMethod.GET)
	public String feignUrlProvider(@PathVariable String str,
			@RequestParam(required = false) String tagName,
			@RequestParam(required = false) String tagValue) {
		if (StringUtils.isNotBlank(tagName)) {
			TsfContext.putTag(tagName, tagValue);
		}
		TsfContext.putTag("operation", "feignUrl");
		Map<String, String> mTags = new HashMap<>();
		mTags.put("feignUrl-trace-key1", "value1");
		mTags.put("feignUrl-trace-key2", "value2");
		TsfContext.putTags(mTags, Tag.ControlFlag.TRANSITIVE);
		return providerService.echo(str);
	}
}
