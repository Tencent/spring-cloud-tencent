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


import java.util.HashSet;
import java.util.Set;

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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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

	@Autowired
	private WebTestClient webClient;

	@Test
	public void fallback() throws Exception {

		stubFor(get(urlEqualTo("/err"))
				.willReturn(aResponse()
						.withStatus(500)
						.withBody("err")
						.withFixedDelay(3000)));

		webClient
				.get().uri("/err")
				.header("Host", "www.circuitbreaker.com")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.consumeWith(
						response -> assertThat(response.getResponseBody()).isEqualTo("fallback".getBytes()));
	}


	@Configuration
	@EnableAutoConfiguration
	public static class TestApplication {

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
