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

package com.tencent.cloud.polaris.circuitbreaker.instrument.resttemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.google.protobuf.util.JsonFormat;
import com.tencent.cloud.polaris.circuitbreaker.config.PolarisCircuitBreakerFeignClientAutoConfiguration;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * @author sean yu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = PolarisCircuitBreakerRestTemplateIntegrationTest.TestConfig.class,
		properties = {
				"spring.cloud.gateway.enabled=false",
				"spring.cloud.polaris.address=grpc://127.0.0.1:10081",
				"feign.circuitbreaker.enabled=true",
				"spring.cloud.polaris.namespace=" + NAMESPACE_TEST,
				"spring.cloud.polaris.service=test"
		})
public class PolarisCircuitBreakerRestTemplateIntegrationTest {

	private static final String TEST_SERVICE_NAME = "test-service-callee";

	private static NamingServer namingServer;

	@Autowired
	@Qualifier("defaultRestTemplate")
	private RestTemplate defaultRestTemplate;

	@Autowired
	@Qualifier("restTemplateFallbackFromPolaris")
	private RestTemplate restTemplateFallbackFromPolaris;

	@BeforeAll
	static void beforeAll() throws Exception {
		PolarisSDKContextManager.innerDestroy();
		namingServer = NamingServer.startNamingServer(10081);
		ServiceKey serviceKey = new ServiceKey(NAMESPACE_TEST, TEST_SERVICE_NAME);
		CircuitBreakerProto.CircuitBreakerRule.Builder circuitBreakerRuleBuilder = CircuitBreakerProto.CircuitBreakerRule.newBuilder();
		InputStream inputStream = PolarisCircuitBreakerRestTemplateIntegrationTest.class.getClassLoader()
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
	public void testRestTemplate() throws URISyntaxException {
		MockRestServiceServer mockServer = MockRestServiceServer.createServer(defaultRestTemplate);
		mockServer
				.expect(ExpectedCount.once(), requestTo(new URI("http://localhost:18001/example/service/b/info")))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.OK).body("OK"));
		assertThat(defaultRestTemplate.getForObject("http://localhost:18001/example/service/b/info", String.class)).isEqualTo("OK");
		mockServer.verify();
		mockServer.reset();
		HttpHeaders headers = new HttpHeaders();
		mockServer
				.expect(ExpectedCount.once(), requestTo(new URI("http://localhost:18001/example/service/b/info")))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.BAD_GATEWAY).headers(headers).body("BAD_GATEWAY"));
		assertThatThrownBy(() -> {
			defaultRestTemplate.getForObject("http://localhost:18001/example/service/b/info", String.class);
		}).isInstanceOf(HttpServerErrorException.class);
		mockServer.verify();
		mockServer.reset();
		assertThatThrownBy(() -> {
			restTemplateFallbackFromPolaris.getForObject("/example/service/b/info", String.class);
		}).isInstanceOf(IllegalStateException.class);
		assertThat(restTemplateFallbackFromPolaris.getForObject("/example/service/b/info", String.class)).isEqualTo("\"fallback from polaris server\"");
	}

	@Configuration
	@EnableAutoConfiguration
	@ImportAutoConfiguration({PolarisCircuitBreakerFeignClientAutoConfiguration.class})
	@EnableFeignClients
	public static class TestConfig {

		@Bean
		public RestTemplate defaultRestTemplate() {
			return new RestTemplate();
		}

		@Bean
		@LoadBalanced
		public RestTemplate restTemplateFallbackFromPolaris() {
			DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("http://" + TEST_SERVICE_NAME);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setUriTemplateHandler(uriBuilderFactory);
			return restTemplate;
		}

		@RestController
		@RequestMapping("/example/service/b")
		public class ServiceBController {

			/**
			 * Get service information.
			 *
			 * @return service information
			 */
			@GetMapping("/info")
			public String info() {
				return "hello world ! I'm a service B1";
			}

		}

	}
}
