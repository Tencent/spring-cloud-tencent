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

package com.tencent.cloud.quickstart.caller.circuitbreaker;

import com.tencent.cloud.common.metadata.MetadataContext;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Controller for circuit breaker.
 *
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/quickstart/caller/circuitBreak")
public class CircuitBreakerController {

	@Autowired
	private CircuitBreakerQuickstartCalleeService circuitBreakerQuickstartCalleeService;

	@Autowired
	private CircuitBreakerQuickstartCalleeServiceWithFallback circuitBreakerQuickstartCalleeServiceWithFallback;

	@Autowired
	@Qualifier("calleeRestTemplate")
	private RestTemplate calleeRestTemplate;

	@Autowired
	private CircuitBreakerFactory circuitBreakerFactory;

	@Autowired
	private WebClient.Builder webClientBuilder;

	/**
	 * Feign circuit breaker with fallback from Polaris.
	 * @return circuit breaker information of callee
	 */
	@GetMapping("/feign/fallbackFromPolaris")
	public String circuitBreakFeignFallbackFromPolaris() {
		return circuitBreakerQuickstartCalleeService.circuitBreak();
	}

	/**
	 * Feign circuit breaker with fallback from Polaris.
	 * @return circuit breaker information of callee
	 */
	@GetMapping("/feign/fallbackFromCode")
	public String circuitBreakFeignFallbackFromCode() {
		return circuitBreakerQuickstartCalleeServiceWithFallback.circuitBreak();
	}

	/**
	 * Feign circuit breaker with fallback from Polaris.
	 * @return circuit breaker information of callee
	 */
	@GetMapping("/feign/fallbackFromPolaris/wildcard/{uid}")
	public String circuitBreakFeignFallbackFromPolarisWildcard(@PathVariable String uid) {
		return circuitBreakerQuickstartCalleeService.circuitBreakWildcard(uid);
	}

	/**
	 * Feign circuit breaker with fallback from Polaris.
	 * @return circuit breaker information of callee
	 */
	@GetMapping("/feign/fallbackFromCode/wildcard/{uid}")
	public String circuitBreakFeignFallbackFromCodeWildcard(@PathVariable String uid) {
		return circuitBreakerQuickstartCalleeServiceWithFallback.circuitBreakWildcard(uid);
	}

	/**
	 * RestTemplate circuit breaker.
	 * @return circuit breaker information of callee
	 */
	@GetMapping("/rest")
	public String circuitBreakRestTemplate() {
		return circuitBreakerFactory
				.create(MetadataContext.LOCAL_NAMESPACE + "#QuickstartCalleeService#/quickstart/callee/circuitBreak#http#GET")
				.run(() -> calleeRestTemplate.getForObject("/quickstart/callee/circuitBreak", String.class),
						throwable -> "trigger the refuse for service callee."
				);
	}

	/**
	 * RestTemplate wildcard circuit breaker with fallback from Polaris.
	 * @return circuit breaker information of callee
	 */
	@GetMapping("/rest/fallbackFromPolaris/wildcard/{uid}")
	public ResponseEntity<String> circuitBreakRestTemplateFallbackFromPolarisWildcard(@PathVariable String uid) {
		String path = String.format("/quickstart/callee/circuitBreak/wildcard/%s", uid);
		return calleeRestTemplate.getForEntity(path, String.class);
	}

	/**
	 * RestTemplate circuit breaker with fallback from Polaris.
	 * @return circuit breaker information of callee
	 */
	@GetMapping("/rest/fallbackFromPolaris")
	public ResponseEntity<String> circuitBreakRestTemplateFallbackFromPolaris() {
		try {
			return calleeRestTemplate.getForEntity("/quickstart/callee/circuitBreak", String.class);
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
				.uri("/quickstart/callee/circuitBreak")
				.retrieve()
				.bodyToMono(String.class);
	}
}
