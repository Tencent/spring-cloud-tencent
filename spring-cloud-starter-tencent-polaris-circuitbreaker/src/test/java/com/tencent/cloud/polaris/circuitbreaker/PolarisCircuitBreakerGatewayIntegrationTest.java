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

package com.tencent.cloud.polaris.circuitbreaker;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author sean yu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.cloud.gateway.enabled=true",
				"spring.cloud.polaris.namespace=default",
				"spring.cloud.polaris.service=Test",
				"spring.main.web-application-type=reactive",
				"httpbin=http://localhost:${wiremock.server.port}"
		},
		classes = PolarisCircuitBreakerGatewayIntegrationTest.TestApplication.class
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test-gateway")
@AutoConfigureWebTestClient(timeout = "10000")
public class PolarisCircuitBreakerGatewayIntegrationTest {

	private static final String TEST_SERVICE_NAME = "test-service-callee";

	@Autowired
	private WebTestClient webClient;

	private static NamingServer namingServer;

	@AfterAll
	public static void afterAll() {
		if (null != namingServer) {
			namingServer.terminate();
		}
	}

	@Test
	public void fallback() throws Exception {

		webClient
				.get().uri("/err")
				.header("Host", "www.circuitbreaker.com")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.consumeWith(
						response -> assertThat(response.getResponseBody()).isEqualTo("fallback".getBytes()));

		webClient
				.get().uri("/err-no-fallback")
				.header("Host", "www.circuitbreaker-no-fallback.com")
				.exchange()
				.expectStatus().isEqualTo(500);

		Utils.sleepUninterrupted(2000);

		webClient
				.get().uri("/err-no-fallback")
				.header("Host", "www.circuitbreaker-no-fallback.com")
				.exchange()
				.expectStatus().isEqualTo(200);
	}


	@Configuration
	@EnableAutoConfiguration
	public static class TestApplication {

		@Bean
		public CircuitBreakAPI circuitBreakAPI() throws InvalidProtocolBufferException {
			try {
				namingServer = NamingServer.startNamingServer(10081);
				System.setProperty(SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
			}
			catch (IOException e) {

			}
			ServiceKey serviceKey = new ServiceKey("default", TEST_SERVICE_NAME);

			CircuitBreakerProto.CircuitBreakerRule.Builder circuitBreakerRuleBuilder =  CircuitBreakerProto.CircuitBreakerRule.newBuilder();
			InputStream inputStream = PolarisCircuitBreakerMockServerTest.class.getClassLoader().getResourceAsStream("circuitBreakerRule.json");
			String json = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining(""));
			JsonFormat.parser().ignoringUnknownFields().merge(json, circuitBreakerRuleBuilder);
			CircuitBreakerProto.CircuitBreakerRule circuitBreakerRule = circuitBreakerRuleBuilder.build();
			CircuitBreakerProto.CircuitBreaker circuitBreaker = CircuitBreakerProto.CircuitBreaker.newBuilder().addRules(circuitBreakerRule).build();
			namingServer.getNamingService().setCircuitBreaker(serviceKey, circuitBreaker);
			com.tencent.polaris.api.config.Configuration configuration = TestUtils.configWithEnvAddress();
			return CircuitBreakAPIFactory.createCircuitBreakAPIByConfig(configuration);
		}

		@Bean
		public RouteLocator myRoutes(RouteLocatorBuilder builder) {
			String httpUri = "http://httpbin.org:80";
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
							.uri(httpUri))
					.route(p -> p
							.host("*.circuitbreaker-no-fallback.com")
							.filters(f -> f
									.circuitBreaker(config -> config
											.setName(TEST_SERVICE_NAME)
									))
							.uri(httpUri))
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
