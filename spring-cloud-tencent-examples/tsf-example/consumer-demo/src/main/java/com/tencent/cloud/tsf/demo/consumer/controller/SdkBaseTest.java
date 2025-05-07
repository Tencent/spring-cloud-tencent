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

import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.tsf.core.TsfContext;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class SdkBaseTest {

	private static final Logger LOG = LoggerFactory.getLogger(SdkBaseTest.class);

	@Autowired
	private RestTemplate restTemplate;

	// 调用一次provider接口
	@RequestMapping(value = "/echo-once/{str}", method = RequestMethod.GET)
	public String restOnceProvider(@PathVariable String str,
			@RequestParam(required = false) String tagName,
			@RequestParam(required = false) String tagValue) {
		if (!StringUtils.isEmpty(tagName)) {
			TsfContext.putTag(tagName, tagValue);
		}
		LOG.info("start call echo-once");
		String result = restTemplate.getForObject("http://provider-demo/echo/" + str, String.class);
		LOG.info("end call echo-once, the result is : " + result);
		return result;
	}
}
