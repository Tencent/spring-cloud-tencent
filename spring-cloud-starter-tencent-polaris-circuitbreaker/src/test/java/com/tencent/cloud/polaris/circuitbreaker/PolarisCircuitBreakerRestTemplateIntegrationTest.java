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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.tencent.cloud.polaris.circuitbreaker.config.PolarisCircuitBreakerFeignClientAutoConfiguration;
import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisCircuitBreakerFallback;
import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisCircuitBreakerHttpResponse;
import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisCircuitBreakerRestTemplate;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;
import static org.assertj.core.api.Assertions.assertThat;
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
				"feign.circuitbreaker.enabled=true",
				"spring.cloud.polaris.namespace=default",
				"spring.cloud.polaris.service=test"
		})
@DirtiesContext
public class PolarisCircuitBreakerRestTemplateIntegrationTest {

	private static final String TEST_SERVICE_NAME = "test-service-callee";

	private static NamingServer namingServer;

	@AfterAll
	public static void afterAll() {
		if (null != namingServer) {
			namingServer.terminate();
		}
	}

	@Autowired
	@Qualifier("defaultRestTemplate")
	private RestTemplate defaultRestTemplate;

	@Autowired
	@Qualifier("restTemplateFallbackFromPolaris")
	private RestTemplate restTemplateFallbackFromPolaris;

	@Autowired
	@Qualifier("restTemplateFallbackFromCode")
	private RestTemplate restTemplateFallbackFromCode;

	@Autowired
	@Qualifier("restTemplateFallbackFromCode2")
	private RestTemplate restTemplateFallbackFromCode2;

	@Autowired
	@Qualifier("restTemplateFallbackFromCode3")
	private RestTemplate restTemplateFallbackFromCode3;

	@Autowired
	@Qualifier("restTemplateFallbackFromCode4")
	private RestTemplate restTemplateFallbackFromCode4;

	@Autowired
	private ApplicationContext applicationContext;


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
		mockServer
				.expect(ExpectedCount.once(), requestTo(new URI("http://localhost:18001/example/service/b/info")))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("BAD_GATEWAY"));
		assertThat(defaultRestTemplate.getForObject("http://localhost:18001/example/service/b/info", String.class)).isEqualTo("fallback");
		mockServer.verify();
		mockServer.reset();
		assertThat(restTemplateFallbackFromCode.getForObject("/example/service/b/info", String.class)).isEqualTo("\"this is a fallback class\"");
		Utils.sleepUninterrupted(2000);
		assertThat(restTemplateFallbackFromCode2.getForObject("/example/service/b/info", String.class)).isEqualTo("\"this is a fallback class\"");
		Utils.sleepUninterrupted(2000);
		assertThat(restTemplateFallbackFromCode3.getForEntity("/example/service/b/info", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
		Utils.sleepUninterrupted(2000);
		assertThat(restTemplateFallbackFromCode4.getForObject("/example/service/b/info", String.class)).isEqualTo("fallback");
		Utils.sleepUninterrupted(2000);
		assertThat(restTemplateFallbackFromPolaris.getForObject("/example/service/b/info", String.class)).isEqualTo("\"fallback from polaris server\"");
		// just for code coverage
		PolarisCircuitBreakerHttpResponse response = ((CustomPolarisCircuitBreakerFallback) applicationContext.getBean("customPolarisCircuitBreakerFallback")).fallback();
		assertThat(response.getStatusText()).isEqualTo("OK");
		assertThat(response.getFallbackInfo().getCode()).isEqualTo(200);
	}

	@Configuration
	@EnableAutoConfiguration
	@ImportAutoConfiguration({ PolarisCircuitBreakerFeignClientAutoConfiguration.class })
	@EnableFeignClients
	public static class TestConfig {

		@Bean
		@PolarisCircuitBreakerRestTemplate(fallback = "fallback")
		public RestTemplate defaultRestTemplate() {
			return new RestTemplate();
		}

		@Bean
		@LoadBalanced
		@PolarisCircuitBreakerRestTemplate
		public RestTemplate restTemplateFallbackFromPolaris() {
			DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("http://" + TEST_SERVICE_NAME);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setUriTemplateHandler(uriBuilderFactory);
			return restTemplate;
		}

		@Bean
		@LoadBalanced
		@PolarisCircuitBreakerRestTemplate(fallbackClass = CustomPolarisCircuitBreakerFallback.class)
		public RestTemplate restTemplateFallbackFromCode() {
			DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("http://" + TEST_SERVICE_NAME);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setUriTemplateHandler(uriBuilderFactory);
			return restTemplate;
		}

		@Bean
		@LoadBalanced
		@PolarisCircuitBreakerRestTemplate(fallbackClass = CustomPolarisCircuitBreakerFallback2.class)
		public RestTemplate restTemplateFallbackFromCode2() {
			DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("http://" + TEST_SERVICE_NAME);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setUriTemplateHandler(uriBuilderFactory);
			return restTemplate;
		}

		@Bean
		@LoadBalanced
		@PolarisCircuitBreakerRestTemplate(fallbackClass = CustomPolarisCircuitBreakerFallback3.class)
		public RestTemplate restTemplateFallbackFromCode3() {
			DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("http://" + TEST_SERVICE_NAME);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setUriTemplateHandler(uriBuilderFactory);
			return restTemplate;
		}

		@Bean
		@LoadBalanced
		@PolarisCircuitBreakerRestTemplate(fallback = "fallback")
		public RestTemplate restTemplateFallbackFromCode4() {
			DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("http://" + TEST_SERVICE_NAME);
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setUriTemplateHandler(uriBuilderFactory);
			return restTemplate;
		}

		@Bean
		public CustomPolarisCircuitBreakerFallback customPolarisCircuitBreakerFallback() {
			return new CustomPolarisCircuitBreakerFallback();
		}

		@Bean
		public CustomPolarisCircuitBreakerFallback2 customPolarisCircuitBreakerFallback2() {
			return new CustomPolarisCircuitBreakerFallback2();
		}

		@Bean
		public CustomPolarisCircuitBreakerFallback3 customPolarisCircuitBreakerFallback3() {
			return new CustomPolarisCircuitBreakerFallback3();
		}

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

	public static class CustomPolarisCircuitBreakerFallback implements PolarisCircuitBreakerFallback {
		@Override
		public PolarisCircuitBreakerHttpResponse fallback() {
			return new PolarisCircuitBreakerHttpResponse(
					200,
					new HashMap<String, String>() {{
						put("xxx", "xxx");
					}},
					"\"this is a fallback class\"");
		}
	}

	public static class CustomPolarisCircuitBreakerFallback2 implements PolarisCircuitBreakerFallback {
		@Override
		public PolarisCircuitBreakerHttpResponse fallback() {
			return new PolarisCircuitBreakerHttpResponse(
					200,
					"\"this is a fallback class\""
			);
		}
	}

	public static class CustomPolarisCircuitBreakerFallback3 implements PolarisCircuitBreakerFallback {
		@Override
		public PolarisCircuitBreakerHttpResponse fallback() {
			return new PolarisCircuitBreakerHttpResponse(
					200
			);
		}
	}


}
