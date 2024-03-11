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

package com.tencent.cloud.plugin.lossless;

import java.util.Collections;

import com.tencent.cloud.plugin.lossless.config.LosslessAutoConfiguration;
import com.tencent.cloud.plugin.lossless.config.LosslessPropertiesBootstrapConfiguration;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClientConfiguration;
import com.tencent.cloud.polaris.registry.PolarisRegistration;
import com.tencent.cloud.polaris.registry.PolarisServiceRegistry;
import com.tencent.cloud.polaris.util.OkHttpUtil;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationUtils;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Test for {@link LosslessProxyServiceRegistry}.
 *
 * @author Shedfree Wu
 */
public class LosslessServiceRegistryTest {

	private static String NAMESPACE_TEST = "Test";

	private static String SERVICE_PROVIDER = "java_provider_test";

	private static String HOST = "127.0.0.1";

	private static int APPLICATION_PORT = 19091;

	private static int LOSSLESS_PORT_1 = 28081;

	private static NamingServer namingServer;

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					LosslessAutoConfiguration.class,
					LosslessPropertiesBootstrapConfiguration.class,
					PolarisContextAutoConfiguration.class,
					PolarisPropertiesConfiguration.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisDiscoveryAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.lossless.delayRegisterInterval=5000")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckPath=")
			.withPropertyValues("spring.cloud.polaris.lossless.port=" + LOSSLESS_PORT_1)
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.localIpAddress=" + HOST)
			.withPropertyValues("spring.cloud.polaris.localPort=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues("spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx");

	private final WebApplicationContextRunner contextRunner2 = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					LosslessAutoConfiguration.class,
					LosslessPropertiesBootstrapConfiguration.class,
					PolarisContextAutoConfiguration.class,
					PolarisPropertiesConfiguration.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisDiscoveryAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckInterval=1000")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckPath=/test")
			.withPropertyValues("spring.cloud.polaris.lossless.port=28082")
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.localIpAddress=" + HOST)
			.withPropertyValues("spring.cloud.polaris.localPort=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues("spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx");

	@BeforeAll
	static void beforeAll() throws Exception {
		namingServer = NamingServer.startNamingServer(10081);

		// add service
		namingServer.getNamingService().addService(new ServiceKey(NAMESPACE_TEST, SERVICE_PROVIDER));
	}

	@AfterAll
	static void afterAll() {
		if (null != namingServer) {
			namingServer.terminate();
		}
	}

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	public void testRegister() {
		this.contextRunner.run(context -> {

			AbstractAutoServiceRegistration autoServiceRegistration = context.getBean(AbstractAutoServiceRegistration.class);

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.register(autoServiceRegistration);
			}).doesNotThrowAnyException();
			Thread.sleep(1000);
			// before register online status is false
			assertThatCode(() -> {
				assertThat(OkHttpUtil.checkUrl(HOST, LOSSLESS_PORT_1, "/online", Collections.EMPTY_MAP)).isFalse();
			}).doesNotThrowAnyException();
			// delay register after 5s
			Thread.sleep(5000);
			PolarisServiceRegistry registry = context.getBean(PolarisServiceRegistry.class);
			PolarisRegistration registration = context.getBean(PolarisRegistration.class);

			assertThatCode(() -> {
				assertThat(registry.getStatus(registration)).isEqualTo("DOWN");
			}).doesNotThrowAnyException();

			assertThatCode(() -> {
				assertThat(OkHttpUtil.checkUrl(HOST, LOSSLESS_PORT_1, "/online", Collections.EMPTY_MAP)).isTrue();
			}).doesNotThrowAnyException();

			assertThatCode(() -> {
				assertThat(OkHttpUtil.checkUrl(HOST, LOSSLESS_PORT_1, "/offline", Collections.EMPTY_MAP)).isTrue();
			}).doesNotThrowAnyException();

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.deRegister(autoServiceRegistration);
			}).doesNotThrowAnyException();

			assertThatCode(() -> {
				assertThat(registry.getStatus(registration)).isEqualTo("DOWN");
			}).doesNotThrowAnyException();
		});
	}

	@Test
	public void testRegister2() {
		this.contextRunner2.run(context -> {

			AbstractAutoServiceRegistration autoServiceRegistration = context.getBean(AbstractAutoServiceRegistration.class);

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.register(autoServiceRegistration);
			}).doesNotThrowAnyException();

			Thread.sleep(2000);

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.deRegister(autoServiceRegistration);
			}).doesNotThrowAnyException();
		});
	}


	@Configuration
	@EnableAutoConfiguration
	static class PolarisPropertiesConfiguration {

	}
}
