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

package com.tencent.cloud.lane.caller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/lane/caller")
public class LaneRouterCallerController {

	private static final Logger LOG = LoggerFactory.getLogger(LaneRouterCallerController.class);

	@Value("${server.port:0}")
	private int port;

	@Value("${spring.cloud.client.ip-address:127.0.0.1}")
	private String ip;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private LaneRouterCalleeService quickstartCalleeService;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Value("${service.lane:base}")
	private String lane;

	@Value("${appName:${spring.application.name}}")
	private String appName;

	/**
	 * Get sum of two value.
	 * @param value1 value 1
	 * @param value2 value 2
	 * @return sum
	 */
	@GetMapping("/feign")
	public String feign(@RequestParam int value1, @RequestParam int value2) {
		String value = quickstartCalleeService.sum(value1, value2);
		return String.format("Lane [%s] Caller Service [%s - %s:%s] -> %s", lane, appName, ip, port, value);
	}

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/rest")
	public String rest() {
		String value = restTemplate.getForObject("http://LaneCalleeService/lane/callee/info", String.class);
		return String.format("Lane [%s] Caller Service [%s - %s:%s] -> %s", lane, appName, ip, port, value);
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
	 * Get information of caller.
	 * @return information of caller
	 */
	@GetMapping("/info")
	public String info() {
		LOG.info("Lane {} Service [{} - {}:{}] is called.", lane, appName, ip, port);
		return String.format("Lane [%s] Service [%s - %s:%s] is called.", lane, appName, ip, port);
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
