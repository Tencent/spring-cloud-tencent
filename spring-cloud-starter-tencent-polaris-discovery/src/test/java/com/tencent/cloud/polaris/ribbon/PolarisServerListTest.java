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

package com.tencent.cloud.polaris.ribbon;

import java.util.List;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import com.tencent.cloud.polaris.context.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClientConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisServerList}.
 *
 * @author Haotian Zhang
 */
public class PolarisServerListTest {

	private static NamingServer namingServer;

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(PolarisContextAutoConfiguration.class,
							PolarisServerListTest.PolarisPropertiesConfiguration.class,
							PolarisDiscoveryClientConfiguration.class,
							PolarisDiscoveryAutoConfiguration.class,
							PolarisContextAutoConfiguration.class))
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

	/**
	 * Test {@link PolarisServerList#getInitialListOfServers()} with empty server list.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void test1() {
		this.contextRunner.run(context -> {
			// mock
			IClientConfig iClientConfig = mock(IClientConfig.class);
			when(iClientConfig.getClientName()).thenReturn(SERVICE_PROVIDER);
			PolarisDiscoveryHandler polarisDiscoveryHandler = context
					.getBean(PolarisDiscoveryHandler.class);
			PolarisServerList serverList = new PolarisServerList(polarisDiscoveryHandler);
			serverList.initWithNiwsConfig(iClientConfig);

			List<Server> servers = serverList.getInitialListOfServers();
			assertThat(servers).isEmpty();
		});
	}

	/**
	 * Test {@link PolarisServerList#getUpdatedListOfServers()} with server list of size
	 * 3.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void test2() {
		this.contextRunner.run(context -> {
			// mock
			IClientConfig iClientConfig = mock(IClientConfig.class);
			when(iClientConfig.getClientName()).thenReturn(SERVICE_PROVIDER);
			PolarisDiscoveryHandler polarisDiscoveryHandler = context
					.getBean(PolarisDiscoveryHandler.class);
			PolarisServerList serverList = new PolarisServerList(polarisDiscoveryHandler);
			serverList.initWithNiwsConfig(iClientConfig);

			// add service with 3 instances
			NamingService.InstanceParameter instanceParameter = new NamingService.InstanceParameter();
			instanceParameter.setHealthy(true);
			instanceParameter.setIsolated(false);
			instanceParameter.setWeight(100);
			ServiceKey serviceKey = new ServiceKey(NAMESPACE_TEST, SERVICE_PROVIDER);
			namingServer.getNamingService().batchAddInstances(serviceKey, PORT, 3,
					instanceParameter);

			List<Server> servers = serverList.getUpdatedListOfServers();
			assertThat(servers).hasSize(3);
			assertThat(servers.get(0).getPort()).isEqualTo(PORT);
			assertThat(servers.get(1).getPort()).isEqualTo(PORT + 1);
			assertThat(servers.get(2).getPort()).isEqualTo(PORT + 2);
		});
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableDiscoveryClient
	static class PolarisPropertiesConfiguration {

	}

}
