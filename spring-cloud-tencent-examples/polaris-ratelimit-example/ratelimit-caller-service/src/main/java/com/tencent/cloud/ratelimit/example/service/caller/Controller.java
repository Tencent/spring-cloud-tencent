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

package com.tencent.cloud.ratelimit.example.service.caller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException.TooManyRequests;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestController
@RequestMapping("/business")
public class Controller {

	private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

	private final AtomicInteger index = new AtomicInteger(0);
	private final AtomicLong lastTimestamp = new AtomicLong(0);
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private WebClient.Builder webClientBuilder;

	private final String appName = "RateLimitCalleeService";

	/**
	 * Get information.
	 * @return information
	 */
	@GetMapping("/info")
	public String info() {
		return "hello world for ratelimit service " + index.incrementAndGet();
	}

	@GetMapping("/info/webclient")
	public Mono<String> infoWebClient() {
		return Mono.just("hello world for ratelimit service " + index.incrementAndGet());
	}

	@GetMapping("/invoke/webclient")
	public String invokeInfoWebClient(@RequestParam(defaultValue = "value1", required = false) String value1, @RequestParam(defaultValue = "value1", required = false) String value2, @RequestHeader Map<String, String> headers) throws InterruptedException, ExecutionException {
		StringBuffer builder = new StringBuffer();
		WebClient webClient = webClientBuilder.baseUrl("http://" + appName).build();

		Consumer<HttpHeaders> headersConsumer = httpHeaders -> {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				httpHeaders.add(entry.getKey(), entry.getValue());
			}
		};

		List<Mono<String>> monoList = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			Mono<String> response = webClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/business/info/webclient")
							.queryParam("value1", value1)
							.queryParam("value2", value2)
							.build()
					)
					.headers(headersConsumer)
					.retrieve()
					.bodyToMono(String.class)
					.doOnSuccess(s -> builder.append(s).append("\n"))
					.doOnError(e -> {
						if (e instanceof WebClientResponseException) {
							if (((WebClientResponseException) e).getRawStatusCode() == 429) {
								builder.append("TooManyRequests ").append(index.incrementAndGet()).append("\n");
							}
						}
					})
					.onErrorReturn("");
			monoList.add(response);
		}
		for (Mono<String> mono : monoList) {
			mono.toFuture().get();
		}
		index.set(0);
		return builder.toString();
	}

	/**
	 * Get information 30 times per 1 second.
	 *
	 * @return result of 30 calls.
	 * @throws InterruptedException exception
	 */
	@GetMapping("/invoke")
	public String invokeInfo(@RequestParam(defaultValue = "value1", required = false) String value1, @RequestParam(defaultValue = "value1", required = false) String value2, @RequestHeader Map<String, String> headers) throws InterruptedException {
		StringBuffer builder = new StringBuffer();
		CountDownLatch count = new CountDownLatch(30);
		for (int i = 0; i < 30; i++) {
			new Thread(() -> {
				try {
					HttpHeaders httpHeaders = new HttpHeaders();
					for (Map.Entry<String, String> entry : headers.entrySet()) {
						httpHeaders.add(entry.getKey(), entry.getValue());
					}
					ResponseEntity<String> entity = restTemplate.exchange(
							"http://" + appName + "/business/info?value1={value1}&value2={value2}",
							HttpMethod.GET,
							new HttpEntity<>(httpHeaders),
							String.class,
							value1, value2
					);
					builder.append(entity.getBody()).append("\n");
				}
				catch (RestClientException e) {
					if (e instanceof TooManyRequests) {
						builder.append("TooManyRequests ").append(index.incrementAndGet()).append("\n");
					}
					else {
						throw e;
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				finally {
					count.countDown();
				}
			}).start();
		}
		count.await();
		index.set(0);
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
