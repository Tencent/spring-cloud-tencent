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

package com.tencent.cloud.plugin.lossless;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import com.tencent.cloud.common.util.OkHttpUtil;
import com.tencent.cloud.plugin.lossless.config.LosslessAutoConfiguration;
import com.tencent.cloud.plugin.lossless.config.LosslessPropertiesBootstrapConfiguration;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClientConfiguration;
import com.tencent.cloud.polaris.registry.PolarisRegistration;
import com.tencent.cloud.polaris.registry.PolarisServiceRegistry;
import com.tencent.cloud.rpc.enhancement.transformer.RegistrationTransformer;
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
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Test for {@link LosslessRegistryAspect}.
 *
 * @author Shedfree Wu
 */
public class LosslessRegistryAspectTest {

	private static String NAMESPACE_TEST = "Test";

	private static String SERVICE_PROVIDER = "java_provider_test";

	private static String HOST = "127.0.0.1";

	private static int APPLICATION_PORT = 19091;

	private static int LOSSLESS_PORT_1 = 28083;

	private static NamingServer namingServer;

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					LosslessAutoConfiguration.class,
					LosslessPropertiesBootstrapConfiguration.class,
					PolarisContextAutoConfiguration.class,
					PolarisPropertiesConfiguration.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisDiscoveryAutoConfiguration.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.delayRegisterInterval=5000")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckPath=")
			.withPropertyValues("spring.cloud.polaris.admin.port=" + LOSSLESS_PORT_1)
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
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckInterval=1000")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckPath=/test")
			.withPropertyValues("spring.cloud.polaris.admin.port=28082")
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.localIpAddress=" + HOST)
			.withPropertyValues("spring.cloud.polaris.localPort=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues("spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx");

	private final WebApplicationContextRunner contextRunner3 = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					MockDiscoveryConfiguration.class,
					LosslessAutoConfiguration.class,
					LosslessPropertiesBootstrapConfiguration.class,
					PolarisContextAutoConfiguration.class,
					PolarisPropertiesConfiguration.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisDiscoveryAutoConfiguration.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckInterval=1000")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckPath=/test")
			.withPropertyValues("spring.cloud.polaris.admin.port=28082")
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.localIpAddress=" + HOST)
			.withPropertyValues("spring.cloud.polaris.localPort=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues("spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx")
			.withPropertyValues("spring.autoconfigure.exclude="
					+ "org.springframework.cloud.client.serviceregistry.ServiceRegistryAutoConfiguration");


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
			// delay register after 10s
			Thread.sleep(10000);
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

	@Test
	public void testRegister3() {
		this.contextRunner3.run(context -> {

			AbstractAutoServiceRegistration autoServiceRegistration = context.getBean(AbstractAutoServiceRegistration.class);

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.register(autoServiceRegistration);
			}).doesNotThrowAnyException();

			Thread.sleep(2000);

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.deRegister(autoServiceRegistration);
			}).doesNotThrowAnyException();

			LosslessRegistryAspect losslessRegistryAspect = context.getBean(LosslessRegistryAspect.class);
			Field field = LosslessRegistryAspect.class.getDeclaredField("registrationTransformer");
			field.setAccessible(true);
			RegistrationTransformer registrationTransformer = (RegistrationTransformer) field.get(losslessRegistryAspect);
			assertThat(registrationTransformer.getClass().getName().contains("PolarisRegistrationTransformer"));

			field = LosslessRegistryAspect.class.getDeclaredField("registration");
			field.setAccessible(true);
			Registration registration = (Registration) field.get(losslessRegistryAspect);
			assertThat(registration.getClass().getName().contains("PolarisRegistration"));

			field = LosslessRegistryAspect.class.getDeclaredField("serviceRegistry");
			field.setAccessible(true);
			ServiceRegistry serviceRegistry = (ServiceRegistry) field.get(losslessRegistryAspect);
			assertThat(serviceRegistry.getClass().getName().contains("PolarisServiceRegistry"));
		});
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisPropertiesConfiguration {

	}

	@Configuration
	static class MockDiscoveryConfiguration {
		@Bean
		public ServiceRegistry mockServiceRegistry() {
			return new ServiceRegistry() {
				@Override
				public void register(Registration registration) {

				}

				@Override
				public void deregister(Registration registration) {

				}

				@Override
				public void close() {

				}

				@Override
				public void setStatus(Registration registration, String status) {

				}

				@Override
				public Object getStatus(Registration registration) {
					return null;
				}
			};
		}

		@Bean
		public Registration mockRegistration() {
			return new Registration() {
				@Override
				public String getServiceId() {
					return null;
				}

				@Override
				public String getHost() {
					return null;
				}

				@Override
				public int getPort() {
					return 0;
				}

				@Override
				public boolean isSecure() {
					return false;
				}

				@Override
				public URI getUri() {
					return null;
				}

				@Override
				public Map<String, String> getMetadata() {
					return null;
				}
			};
		}

		@Bean
		public RegistrationTransformer mockRegistrationTransformer() {
			return new RegistrationTransformer() {
				@Override
				public String getRegistry() {
					return null;
				}
			};
		}
	}
}
