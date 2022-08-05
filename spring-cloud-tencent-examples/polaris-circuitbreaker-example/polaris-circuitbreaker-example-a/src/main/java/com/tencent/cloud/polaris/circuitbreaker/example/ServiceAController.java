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

package com.tencent.cloud.polaris.circuitbreaker.example;

import org.owasp.esapi.ESAPI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Circuit breaker example caller controller.
 *
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/example/service/a")
public class ServiceAController {

	@Autowired
	private ProviderB polarisServiceB;

	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Get info of Service B by Feign.
	 * @return info of Service B
	 */
	@GetMapping("/getBServiceInfo")
	public String getBServiceInfo() {
		return polarisServiceB.info();
	}

	@GetMapping("/getBServiceInfoByRestTemplate")
	public String getBServiceInfoByRestTemplate() {
		return restTemplate.getForObject("http://polaris-circuitbreaker-example-b/example/service/b/info", String.class);
	}

	/**
	 * Get info of Service B by RestTemplate.
	 * @return info of Service B
	 */
	@GetMapping("/testRest")
	public String testRest() {
		ResponseEntity<String> entity = restTemplate.getForEntity(
				"http://polaris-circuitbreaker-example-b/example/service/b/info",
				String.class);
		String response = entity.getBody();
		return cleanXSS(response);
	}

	private String cleanXSS(String str) {
		str = ESAPI.encoder().encodeForHTML(str);
		return str;
	}
}
