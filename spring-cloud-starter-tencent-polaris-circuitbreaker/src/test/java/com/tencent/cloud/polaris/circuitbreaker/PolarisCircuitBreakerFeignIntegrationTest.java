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


import com.tencent.cloud.polaris.circuitbreaker.config.PolarisCircuitBreakerFeignClientAutoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author sean yu
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = PolarisCircuitBreakerFeignIntegrationTest.TestConfig.class,
		properties = {
				"spring.cloud.gateway.enabled=false",
				"feign.circuitbreaker.enabled=true",
				"spring.cloud.polaris.namespace=default",
				"spring.cloud.polaris.service=Test"
})
@DirtiesContext
public class PolarisCircuitBreakerFeignIntegrationTest {

	@Autowired
	private EchoService echoService;

	@Autowired
	private FooService fooService;

	@Autowired
	private BarService barService;

	@Autowired
	private BazService bazService;

	@Before
	public void setUp() {
	}

	@Test
	public void contextLoads() throws Exception {
		assertThat(echoService).isNotNull();
		assertThat(fooService).isNotNull();
	}

	@Test
	public void testFeignClient() {
		assertThat(echoService.echo("test")).isEqualTo("echo fallback");
		assertThat(fooService.echo("test")).isEqualTo("foo fallback");

		assertThatThrownBy(() -> {
			barService.bar();
		}).isInstanceOf(Exception.class);

		assertThatThrownBy(() -> {
			bazService.baz();
		}).isInstanceOf(Exception.class);

		assertThat(fooService.toString()).isNotEqualTo(echoService.toString());
		assertThat(fooService.hashCode()).isNotEqualTo(echoService.hashCode());
		assertThat(echoService.equals(fooService)).isEqualTo(Boolean.FALSE);
	}

	@Configuration
	@EnableAutoConfiguration
	@ImportAutoConfiguration({ PolarisCircuitBreakerFeignClientAutoConfiguration.class })
	@EnableFeignClients
	public static class TestConfig {

		@Bean
		public EchoServiceFallback echoServiceFallback() {
			return new EchoServiceFallback();
		}

		@Bean
		public CustomFallbackFactory customFallbackFactory() {
			return new CustomFallbackFactory();
		}

	}

	@FeignClient(value = "test-service", fallback = EchoServiceFallback.class)
	public interface EchoService {

		@RequestMapping(path = "echo/{str}")
		String echo(@RequestParam("str") String param);

	}

	@FeignClient(value = "foo-service", fallbackFactory = CustomFallbackFactory.class)
	public interface FooService {

		@GetMapping("echo/{str}")
		String echo(@RequestParam("str") String param);

	}

	@FeignClient("bar-service")
	public interface BarService {

		@RequestMapping(path = "bar")
		String bar();

	}

	public interface BazService {

		@RequestMapping(path = "baz")
		String baz();

	}

	@FeignClient("baz-service")
	public interface BazClient extends BazService {

	}

	public static class EchoServiceFallback implements EchoService {

		@Override
		public String echo(@RequestParam("str") String param) {
			return "echo fallback";
		}

	}

	public static class FooServiceFallback implements FooService {

		@Override
		public String echo(@RequestParam("str") String param) {
			return "foo fallback";
		}

	}

	public static class CustomFallbackFactory
			implements FallbackFactory<FooService> {

		private FooService fooService = new FooServiceFallback();

		@Override
		public FooService create(Throwable throwable) {
			return fooService;
		}

	}

}
