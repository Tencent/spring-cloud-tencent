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

package com.tencent.cloud.polaris.registry;

import com.tencent.cloud.polaris.context.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClientConfiguration;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisServiceRegistry}.
 *
 * @author Haotian Zhang
 */
public class PolarisServiceRegistryTest {

	private static NamingServer namingServer;

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(PolarisContextAutoConfiguration.class,
							PolarisPropertiesConfiguration.class,
							PolarisDiscoveryClientConfiguration.class,
							PolarisDiscoveryAutoConfiguration.class))
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues(
					"spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx");

	@BeforeClass
	public static void beforeClass() throws Exception {
		namingServer = NamingServer.startNamingServer(10081);

		// add service
		namingServer.getNamingService()
				.addService(new ServiceKey(NAMESPACE_TEST, SERVICE_PROVIDER));
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (null != namingServer) {
			namingServer.terminate();
		}
	}

	@Test
	public void testRegister() {
		this.contextRunner.run(context -> {
			PolarisServiceRegistry registry = context
					.getBean(PolarisServiceRegistry.class);
			PolarisRegistration registration = Mockito.mock(PolarisRegistration.class);
			when(registration.getHost()).thenReturn("127.0.0.1");
			when(registration.getPort()).thenReturn(PORT);
			when(registration.getServiceId()).thenReturn(SERVICE_PROVIDER);

			try {
				registry.register(registration);
			}
			catch (Exception e) {
				fail();
			}

			try {
				assertThat(registry.getStatus(registration)).isEqualTo("DOWN");
			}
			catch (Exception e) {
				fail();
			}

			try {
				registry.deregister(registration);
			}
			catch (Exception e) {
				fail();
			}
		});
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableDiscoveryClient
	static class PolarisPropertiesConfiguration {

	}

}
