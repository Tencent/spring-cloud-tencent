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

package com.tencent.cloud.quickstart.caller;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.discovery.PolarisServiceDiscovery;
import com.tencent.cloud.common.util.PolarisCompletableFutureUtils;
import com.tencent.cloud.quickstart.caller.pojo.User;
import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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

	@Autowired(required = false)
	PolarisServiceDiscovery polarisServiceDiscovery;
	/**
	 * Get sum of two value.
	 * @param value1 value 1
	 * @param value2 value 2
	 * @return sum
	 */
	@GetMapping("/feign")
	public String feign(@RequestParam int value1, @RequestParam int value2) {
		MetadataContext metadataContext = MetadataContextHolder.get();
		metadataContext.setTransitiveMetadata(Collections.singletonMap("feign-trace", String.format("%d+%d", value1, value2)));
		return quickstartCalleeService.sum(value1, value2);
	}

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/rest")
	public ResponseEntity<String> rest(@RequestHeader Map<String, String> headerMap, @RequestParam(required = false) String param) {
		String url = "http://QuickstartCalleeService/quickstart/callee/info?param=" + param;

		HttpHeaders headers = new HttpHeaders();
		for (Map.Entry<String, String> entry : headerMap.entrySet()) {
			if (StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue())
					&& !entry.getKey().contains("sct-")
					&& !entry.getKey().contains("SCT-")
					&& !entry.getKey().contains("polaris-")
					&& !entry.getKey().contains("POLARIS-")) {
				headers.add(entry.getKey(), entry.getValue());
			}
		}

		// 创建 HttpEntity 实例并传入 HttpHeaders
		HttpEntity<String> entity = new HttpEntity<>(headers);

		// 使用 exchange 方法发送 GET 请求，并获取响应
		try {
			return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		}
		catch (HttpClientErrorException | HttpServerErrorException httpClientErrorException) {
			return new ResponseEntity<>(httpClientErrorException.getResponseBodyAsString(), httpClientErrorException.getStatusCode());
		}
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
	 */
	@GetMapping("/ratelimit")
	public String invokeInfo() {
		StringBuilder builder = new StringBuilder();
		AtomicInteger index = new AtomicInteger(0);
		for (int i = 0; i < 30; i++) {
			try {
				ResponseEntity<String> entity = restTemplate.getForEntity(
						"http://QuickstartCalleeService/quickstart/callee/info", String.class);
				builder.append(entity.getBody() + "\n");
				try {
					Thread.sleep(30);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			catch (RestClientException e) {
				if (e instanceof HttpClientErrorException.TooManyRequests) {
					builder.append("TooManyRequests " + index.incrementAndGet() + "\n");
				}
				else {
					throw e;
				}
			}
		}

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

	/**
	 * health check.
	 * @return health check info
	 */
	@GetMapping("/test/{num}")
	public String test1(@PathVariable int num) {
		String path = "http://QuickstartCalleeService/quickstart/callee/test/" + num + "/echo";
		return restTemplate.getForObject(path, String.class);
	}

	/**
	 * Get information of callee with async and delay.
	 * @param headerMap request headers
	 * @param delay delay time in milliseconds
	 * @return information of callee
	 */
	@GetMapping("/rest-async")
	public ResponseEntity<String> restAsync(@RequestHeader Map<String, String> headerMap,
			@RequestParam(defaultValue = "1000") long delay) {
		String url = "http://QuickstartCalleeService/quickstart/callee/info";

		HttpHeaders headers = new HttpHeaders();
		for (Map.Entry<String, String> entry : headerMap.entrySet()) {
			if (StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue())
					&& !entry.getKey().contains("sct-")
					&& !entry.getKey().contains("SCT-")
					&& !entry.getKey().contains("polaris-")
					&& !entry.getKey().contains("POLARIS-")) {
				headers.add(entry.getKey(), entry.getValue());
			}
		}

		HttpEntity<String> entity = new HttpEntity<>(headers);

		PolarisCompletableFutureUtils.runAsync(() -> {
			try {
				TimeUnit.MILLISECONDS.sleep(delay);
				LOG.info("begin rest");
				ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
				LOG.info("result: {}", result.getBody());
			}
			catch (HttpClientErrorException | HttpServerErrorException httpClientErrorException) {
				LOG.error("HttpClientErrorException: {}", httpClientErrorException.getResponseBodyAsString());
			}
			catch (InterruptedException e) {
				LOG.error("InterruptedException: {}", e.getMessage());
			}
			catch (Throwable t) {
				LOG.error("Throwable", t);
			}
		});

		LOG.info("return ok");
		return new ResponseEntity<>("ok", HttpStatus.OK);
	}

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/rest-self")
	public ResponseEntity<String> restSelf(@RequestHeader Map<String, String> headerMap) {
		String url = "http://QuickstartCallerService/quickstart/caller/rest";

		HttpHeaders headers = new HttpHeaders();
		for (Map.Entry<String, String> entry : headerMap.entrySet()) {
			if (StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue())
					&& !entry.getKey().contains("sct-")
					&& !entry.getKey().contains("SCT-")
					&& !entry.getKey().contains("polaris-")
					&& !entry.getKey().contains("POLARIS-")) {
				headers.add(entry.getKey(), entry.getValue());
			}
		}

		// 创建 HttpEntity 实例并传入 HttpHeaders
		HttpEntity<String> entity = new HttpEntity<>(headers);

		// 使用 exchange 方法发送 GET 请求，并获取响应
		try {
			return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		}
		catch (HttpClientErrorException | HttpServerErrorException httpClientErrorException) {
			return new ResponseEntity<>(httpClientErrorException.getResponseBodyAsString(), httpClientErrorException.getStatusCode());
		}
	}

	@PostMapping("/user")
	public String user(@RequestBody User user) {
		return restTemplate.postForObject("http://QuickstartCalleeService/quickstart/callee/user", user, String.class);
	}

	@GetMapping(value = "/getServices", produces = "application/json")
	public String services() {
		try {
			return JacksonUtils.serialize2Json(polarisServiceDiscovery.getServices());
		}
		catch (NullPointerException e) {
			return "Polaris Discovery is not enabled. Please set spring.cloud.polaris.discovery.enabled=true";
		}
	}
}
