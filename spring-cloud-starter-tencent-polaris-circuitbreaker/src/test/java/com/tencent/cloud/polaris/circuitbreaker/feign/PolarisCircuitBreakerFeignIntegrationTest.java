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

package com.tencent.cloud.polaris.circuitbreaker.feign;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.protobuf.util.JsonFormat;
import com.tencent.cloud.polaris.circuitbreaker.PolarisCircuitBreakerFactory;
import com.tencent.cloud.polaris.circuitbreaker.config.PolarisCircuitBreakerFeignClientAutoConfiguration;
import com.tencent.cloud.polaris.circuitbreaker.reporter.ExceptionCircuitBreakerReporter;
import com.tencent.cloud.polaris.circuitbreaker.reporter.SuccessCircuitBreakerReporter;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author sean yu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = PolarisCircuitBreakerFeignIntegrationTest.TestConfig.class,
		properties = {
				"spring.cloud.gateway.enabled=false",
				"feign.circuitbreaker.enabled=true",
				"spring.cloud.polaris.namespace=" + NAMESPACE_TEST,
				"spring.cloud.polaris.service=test"
		})
public class PolarisCircuitBreakerFeignIntegrationTest {

	private static final String TEST_SERVICE_NAME = "test-service-callee";

	@Autowired
	private EchoService echoService;

	@Autowired
	private FooService fooService;

	@Autowired
	private BarService barService;

	@Autowired
	private BazService bazService;

	@Test
	public void contextLoads() {
		assertThat(echoService).isNotNull();
		assertThat(fooService).isNotNull();
	}

	@Test
	public void testFeignClient() throws InvocationTargetException {
		assertThat(echoService.echo("test")).isEqualTo("echo fallback");
		Utils.sleepUninterrupted(2000);
		assertThatThrownBy(() -> {
			echoService.echo(null);
		}).isInstanceOf(InvocationTargetException.class);
		assertThatThrownBy(() -> {
			fooService.echo("test");
		}).isInstanceOf(NoFallbackAvailableException.class);
		Utils.sleepUninterrupted(2000);
		assertThat(barService.bar()).isEqualTo("\"fallback from polaris server\"");
		Utils.sleepUninterrupted(2000);
		assertThat(bazService.baz()).isEqualTo("\"fallback from polaris server\"");
		assertThat(fooService.toString()).isNotEqualTo(echoService.toString());
		assertThat(fooService.hashCode()).isNotEqualTo(echoService.hashCode());
		assertThat(echoService.equals(fooService)).isEqualTo(Boolean.FALSE);
	}

	@FeignClient(value = TEST_SERVICE_NAME, contextId = "1", fallback = EchoServiceFallback.class)
	@Primary
	public interface EchoService {

		@RequestMapping(path = "echo/{str}")
		String echo(@RequestParam("str") String param) throws InvocationTargetException;

	}

	@FeignClient(value = TEST_SERVICE_NAME, contextId = "2", fallbackFactory = CustomFallbackFactory.class)
	public interface FooService {

		@RequestMapping("echo/{str}")
		String echo(@RequestParam("str") String param);

	}

	@FeignClient(value = TEST_SERVICE_NAME, contextId = "3")
	public interface BarService {

		@RequestMapping(path = "bar")
		String bar();

	}

	public interface BazService {

		@RequestMapping(path = "baz")
		String baz();

	}

	@FeignClient(value = TEST_SERVICE_NAME, contextId = "4")
	public interface BazClient extends BazService {

	}

	@Configuration
	@EnableAutoConfiguration
	@ImportAutoConfiguration({PolarisCircuitBreakerFeignClientAutoConfiguration.class})
	@EnableFeignClients
	public static class TestConfig {

		@Autowired(required = false)
		private List<Customizer<PolarisCircuitBreakerFactory>> customizers = new ArrayList<>();

		@Bean
		public EchoServiceFallback echoServiceFallback() {
			return new EchoServiceFallback();
		}

		@Bean
		public CustomFallbackFactory customFallbackFactory() {
			return new CustomFallbackFactory();
		}

		@Bean
		public PreDestroy preDestroy(NamingServer namingServer) {
			return new PreDestroy(namingServer);
		}

		@Bean
		public NamingServer namingServer() throws IOException {
			NamingServer namingServer = NamingServer.startNamingServer(-1);
			System.setProperty(SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
			ServiceKey serviceKey = new ServiceKey(NAMESPACE_TEST, TEST_SERVICE_NAME);

			CircuitBreakerProto.CircuitBreakerRule.Builder circuitBreakerRuleBuilder = CircuitBreakerProto.CircuitBreakerRule.newBuilder();
			InputStream inputStream = PolarisCircuitBreakerFeignIntegrationTest.class.getClassLoader()
					.getResourceAsStream("circuitBreakerRule.json");
			String json = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
					.collect(Collectors.joining(""));
			JsonFormat.parser().ignoringUnknownFields().merge(json, circuitBreakerRuleBuilder);
			CircuitBreakerProto.CircuitBreakerRule circuitBreakerRule = circuitBreakerRuleBuilder.build();
			CircuitBreakerProto.CircuitBreaker circuitBreaker = CircuitBreakerProto.CircuitBreaker.newBuilder()
					.addRules(circuitBreakerRule).build();
			namingServer.getNamingService().setCircuitBreaker(serviceKey, circuitBreaker);
			return namingServer;
		}

		@Bean
		public CircuitBreakAPI circuitBreakAPI(NamingServer namingServer) {
			com.tencent.polaris.api.config.Configuration configuration = TestUtils.configWithEnvAddress();
			return CircuitBreakAPIFactory.createCircuitBreakAPIByConfig(configuration);
		}

		@Bean
		public ConsumerAPI consumerAPI(NamingServer namingServer) {
			com.tencent.polaris.api.config.Configuration configuration = TestUtils.configWithEnvAddress();
			return DiscoveryAPIFactory.createConsumerAPIByConfig(configuration);
		}

		@Bean
		@ConditionalOnMissingBean(SuccessCircuitBreakerReporter.class)
		public SuccessCircuitBreakerReporter successCircuitBreakerReporter(RpcEnhancementReporterProperties properties,
				CircuitBreakAPI circuitBreakAPI) {
			return new SuccessCircuitBreakerReporter(properties, circuitBreakAPI);
		}

		@Bean
		@ConditionalOnMissingBean(ExceptionCircuitBreakerReporter.class)
		public ExceptionCircuitBreakerReporter exceptionCircuitBreakerReporter(RpcEnhancementReporterProperties properties,
				CircuitBreakAPI circuitBreakAPI) {
			return new ExceptionCircuitBreakerReporter(properties, circuitBreakAPI);
		}

		@Bean
		@ConditionalOnMissingBean(CircuitBreakerFactory.class)
		public CircuitBreakerFactory polarisCircuitBreakerFactory(CircuitBreakAPI circuitBreakAPI, ConsumerAPI consumerAPI) {
			PolarisCircuitBreakerFactory factory = new PolarisCircuitBreakerFactory(circuitBreakAPI, consumerAPI);
			customizers.forEach(customizer -> customizer.customize(factory));
			return factory;
		}
	}

	public static class EchoServiceFallback implements EchoService {

		@Override
		public String echo(@RequestParam("str") String param) throws InvocationTargetException {
			if (param == null) {
				throw new InvocationTargetException(new Exception(), "test InvocationTargetException");
			}
			return "echo fallback";
		}

	}

	public static class FooServiceFallback implements FooService {

		@Override
		public String echo(@RequestParam("str") String param) {
			throw new NoFallbackAvailableException("fallback", new RuntimeException());
		}

	}

	public static class CustomFallbackFactory
			implements FallbackFactory<FooService> {

		private final FooService fooService = new FooServiceFallback();

		@Override
		public FooService create(Throwable throwable) {
			return fooService;
		}

	}

	public static class PreDestroy implements DisposableBean {

		private final NamingServer namingServer;

		public PreDestroy(NamingServer namingServer) {
			this.namingServer = namingServer;
		}

		@Override
		public void destroy() throws Exception {
			namingServer.terminate();
		}
	}

}
