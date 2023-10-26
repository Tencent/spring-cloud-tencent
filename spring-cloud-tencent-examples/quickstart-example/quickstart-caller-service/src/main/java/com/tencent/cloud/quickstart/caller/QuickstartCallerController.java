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

package com.tencent.cloud.quickstart.caller;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Quickstart caller controller.
 *
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/quickstart/caller")
public class QuickstartCallerController {

	private static final Logger LOG = LoggerFactory.getLogger(QuickstartCallerController.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private QuickstartCalleeService quickstartCalleeService;

	@Autowired
	private WebClient.Builder webClientBuilder;

	/**
	 * Get sum of two value.
	 * @param value1 value 1
	 * @param value2 value 2
	 * @return sum
	 */
	@GetMapping("/feign")
	public String feign(@RequestParam int value1, @RequestParam int value2) {
		return quickstartCalleeService.sum(value1, value2);
	}

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/rest")
	public String rest() {
		return restTemplate.getForObject("http://QuickstartCalleeService/quickstart/callee/info", String.class);
	}

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/webclient")
	public Mono<String> webclient() {
		return webClientBuilder
				.build()
				.get()
				.uri("/quickstart/callee/echo")
				.retrieve()
				.bodyToMono(String.class);
	}

	/**
	 * Get information 30 times per 1 second.
	 *
	 * @return result of 30 calls.
	 * @throws InterruptedException exception
	 */
	@GetMapping("/ratelimit")
	public String invokeInfo() throws InterruptedException {
		StringBuffer builder = new StringBuffer();
		CountDownLatch count = new CountDownLatch(30);
		AtomicInteger index = new AtomicInteger(0);
		for (int i = 0; i < 30; i++) {
			new Thread(() -> {
				try {
					ResponseEntity<String> entity = restTemplate.getForEntity(
							"http://QuickstartCalleeService/quickstart/callee/info", String.class);
					builder.append(entity.getBody() + "\n");
				}
				catch (RestClientException e) {
					if (e instanceof HttpClientErrorException.TooManyRequests) {
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
	public String unirate() throws InterruptedException {
		StringBuffer builder = new StringBuffer();
		CountDownLatch count = new CountDownLatch(30);
		AtomicInteger index = new AtomicInteger(0);
		long currentTimestamp = System.currentTimeMillis();
		for (int i = 0; i < 30; i++) {
			new Thread(() -> {
				try {
					long startTimestamp = System.currentTimeMillis();
					ResponseEntity<String> entity = restTemplate.getForEntity(
							"http://QuickstartCalleeService/quickstart/callee/info", String.class);
					long endTimestamp = System.currentTimeMillis();
					builder.append("Start timestamp:" + startTimestamp + ". End timestamp: " + endTimestamp +
							". diff interval:" + (endTimestamp - startTimestamp) + "\n");
				}
				catch (RestClientException e) {
					if (e instanceof HttpClientErrorException.TooManyRequests) {
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
		long lastTimestamp = System.currentTimeMillis();
		builder.append("Unirate request from " + currentTimestamp + " to " + lastTimestamp + " with interval " + (lastTimestamp - currentTimestamp) + "ms.");

		return builder.toString();
	}

	/**
	 * Get information of caller.
	 * @return information of caller
	 */
	@GetMapping("/info")
	public String info() {
		LOG.info("Quickstart Callee Service is called.");
		return "Quickstart Callee Service is called.";
	}

	/**
	 * health check.
	 * @return health check info
	 */
	@GetMapping("/healthCheck")
	public String healthCheck() {
		return "ok";
	}
}
