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


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@ExtendWith(SpringExtension.class)
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.cloud.gateway.enabled=true",
				"spring.cloud.polaris.namespace=default",
				"spring.cloud.polaris.service=Test",
				"spring.main.web-application-type=reactive"
		},
		classes = PolarisCircuitBreakerGatewayIntegrationTest.TestApplication.class
)
@ActiveProfiles("test-gateway")
public class PolarisCircuitBreakerGatewayIntegrationTest {

	@Autowired
	private ApplicationContext context;

	@Test
	public void contextLoads() throws Exception {
		WebTestClient client = WebTestClient.bindToApplicationContext(this.context)
				.build();
		client.get().uri("/hello/1").exchange().expectStatus().isOk();
	}


	@Configuration
	@EnableAutoConfiguration
	public static class TestApplication {

		@RestController
		static class Controller {

			@GetMapping("/hello/{id}")
			public Mono<String> hello(@PathVariable Integer id) {
				return Mono.just("hello" + id);
			}

		}

	}

}
