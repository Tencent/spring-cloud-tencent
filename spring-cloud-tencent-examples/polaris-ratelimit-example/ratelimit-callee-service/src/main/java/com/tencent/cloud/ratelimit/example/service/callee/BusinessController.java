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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Rate limit controller.
 *
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/business")
public class BusinessController {

	private static final Logger LOG = LoggerFactory.getLogger(BusinessController.class);

	private final AtomicInteger index = new AtomicInteger(0);
	private final AtomicLong lastTimestamp = new AtomicLong(0);
	@Autowired
	private RestTemplate restTemplate;
	@Value("${spring.application.name}")
	private String appName;

	/**
	 * Get information.
	 *
	 * @return information
	 */
	@GetMapping("/info")
	public String info() {
		return "hello world for ratelimit service " + index.incrementAndGet();
	}

	/**
	 * Get information 30 times per 1 second.
	 *
	 * @return result of 30 calls.
	 * @throws InterruptedException exception
	 */
	@GetMapping("/invoke")
	public String invokeInfo() throws InterruptedException {
		StringBuffer builder = new StringBuffer();
		CountDownLatch count = new CountDownLatch(30);
		for (int i = 0; i < 30; i++) {
			new Thread(() -> {
				try {
					ResponseEntity<String> entity = restTemplate.getForEntity("http://" + appName + "/business/info",
							String.class);
					builder.append(entity.getBody() + "\n");
				}
				catch (RestClientException e) {
					if (e instanceof TooManyRequests) {
						builder.append("TooManyRequests " + index.incrementAndGet() + "\n");
					}
					else {
						throw e;
					}
				}
				count.countDown();
			}).start();
		}
		count.await();
		return builder.toString();
	}

	/**
	 * Get information with unirate.
	 *
	 * @return information
	 */
	@GetMapping("/unirate")
	public String unirate() {
		long currentTimestamp = System.currentTimeMillis();
		long lastTime = lastTimestamp.get();
		if (lastTime != 0) {
			LOG.info("Current timestamp:" + currentTimestamp + ", diff from last timestamp:" + (currentTimestamp - lastTime));
		}
		else {
			LOG.info("Current timestamp:" + currentTimestamp);
		}
		lastTimestamp.set(currentTimestamp);
		return "hello world for ratelimit service with diff from last request:" + (currentTimestamp - lastTime) + "ms.";
	}
}
