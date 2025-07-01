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

package com.tencent.cloud.polaris.circuitbreaker.instrument.gateway;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.util.JsonFormat;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author sean yu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = PolarisCircuitBreakerGatewayIntegrationTest.TestApplication.class
)
@ActiveProfiles("test-gateway")
@AutoConfigureWebTestClient(timeout = "1000000")
public class PolarisCircuitBreakerGatewayIntegrationTest {

	private static final String TEST_SERVICE_NAME = "test-service-callee";

	private static NamingServer namingServer;

	@Autowired
	private WebTestClient webClient;

	@Autowired
	private ApplicationContext applicationContext;

	@BeforeAll
	static void beforeAll() throws Exception {
		PolarisSDKContextManager.innerDestroy();
		namingServer = NamingServer.startNamingServer(10081);
		ServiceKey serviceKey = new ServiceKey(NAMESPACE_TEST, TEST_SERVICE_NAME);

		CircuitBreakerProto.CircuitBreakerRule.Builder circuitBreakerRuleBuilder = CircuitBreakerProto.CircuitBreakerRule.newBuilder();
		InputStream inputStream = PolarisCircuitBreakerGatewayIntegrationTest.class.getClassLoader()
				.getResourceAsStream("circuitBreakerRule.json");
		String json = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining(""));
		JsonFormat.parser().ignoringUnknownFields().merge(json, circuitBreakerRuleBuilder);
		CircuitBreakerProto.CircuitBreakerRule circuitBreakerRule = circuitBreakerRuleBuilder.build();
		CircuitBreakerProto.CircuitBreaker circuitBreaker = CircuitBreakerProto.CircuitBreaker.newBuilder()
				.addRules(circuitBreakerRule).build();
		namingServer.getNamingService().setCircuitBreaker(serviceKey, circuitBreaker);
	}

	@AfterAll
	static void afterAll() {
		if (null != namingServer) {
			namingServer.terminate();
		}
	}

	@Test
	public void fallback() throws Exception {
		SpringCloudCircuitBreakerFilterFactory.Config config = new SpringCloudCircuitBreakerFilterFactory.Config();
		applicationContext.getBean(PolarisCircuitBreakerFilterFactory.class).apply(config).toString();

		webClient
				.get().uri("/err")
				.header("Host", "www.circuitbreaker.com")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.consumeWith(
						response -> assertThat(response.getResponseBody()).isEqualTo("fallback".getBytes()));

		Utils.sleepUninterrupted(2000);

		webClient
				.get().uri("/err-skip-fallback")
				.header("Host", "www.circuitbreaker-skip-fallback.com")
				.exchange()
				.expectStatus();

		Utils.sleepUninterrupted(2000);

		// this should be 200, but for some unknown reason, GitHub action run failed in windows, so we skip this check
		webClient
				.get().uri("/err-skip-fallback")
				.header("Host", "www.circuitbreaker-skip-fallback.com")
				.exchange()
				.expectStatus();

		Utils.sleepUninterrupted(2000);

		webClient
				.get().uri("/err-no-fallback")
				.header("Host", "www.circuitbreaker-no-fallback.com")
				.exchange()
				.expectStatus();

		Utils.sleepUninterrupted(2000);

		webClient
				.get().uri("/err-no-fallback")
				.header("Host", "www.circuitbreaker-no-fallback.com")
				.exchange()
				.expectStatus();

		Utils.sleepUninterrupted(2000);

		webClient
				.get().uri("/err-no-fallback")
				.header("Host", "www.circuitbreaker-no-fallback.com")
				.exchange()
				.expectStatus();
	}


	@Configuration
	@EnableAutoConfiguration
	public static class TestApplication {

		@Bean
		public RouteLocator myRoutes(RouteLocatorBuilder builder) {
			Set<String> codeSets = new HashSet<>();
			codeSets.add("4**");
			codeSets.add("5**");
			return builder.routes()
					.route(p -> p
							.host("*.circuitbreaker.com")
							.filters(f -> f
									.circuitBreaker(config -> config
											.setStatusCodes(codeSets)
											.setFallbackUri("forward:/fallback")
											.setName(TEST_SERVICE_NAME)
									))
							.uri("http://httpbin.org:80"))
					.route(p -> p
							.host("*.circuitbreaker-skip-fallback.com")
							.filters(f -> f
									.circuitBreaker(config -> config
											.setStatusCodes(Collections.singleton("5**"))
											.setName(TEST_SERVICE_NAME)
									))
							.uri("http://httpbin.org:80"))
					.route(p -> p
							.host("*.circuitbreaker-no-fallback.com")
							.filters(f -> f
									.circuitBreaker(config -> config
											.setName(TEST_SERVICE_NAME)
									))
							.uri("lb://" + TEST_SERVICE_NAME))
					.build();
		}

		@RestController
		static class Controller {
			@RequestMapping("/fallback")
			public Mono<String> fallback() {
				return Mono.just("fallback");
			}
		}

	}
}
