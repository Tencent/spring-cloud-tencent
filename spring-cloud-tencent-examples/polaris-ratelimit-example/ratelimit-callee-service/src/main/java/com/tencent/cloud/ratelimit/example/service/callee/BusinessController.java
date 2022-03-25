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

package com.tencent.cloud.ratelimit.example.service.callee;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException.TooManyRequests;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/business")
public class BusinessController {

	private final AtomicInteger index = new AtomicInteger(0);

	@Autowired
	private RestTemplate restTemplate;

	@Value("${spring.application.name}")
	private String appName;

	/**
	 * 获取当前服务的信息
	 * @return 返回服务信息
	 */
	@GetMapping("/info")
	public String info() {
		return "hello world for ratelimit service " + index.incrementAndGet();
	}

	@GetMapping("/invoke")
	public String invokeInfo() throws Exception {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 30; i++) {
			try {
				ResponseEntity<String> entity = restTemplate.getForEntity(
						"http://" + appName + "/business/info", String.class);
				builder.append(entity.getBody()).append("<br/>");
			}
			catch (RestClientException e) {
				if (e instanceof TooManyRequests) {
					builder.append(((TooManyRequests) e).getResponseBodyAsString())
							.append(index.incrementAndGet()).append("<br/>");
				}
				else {
					throw e;
				}
			}
		}
		return builder.toString();
	}

}
