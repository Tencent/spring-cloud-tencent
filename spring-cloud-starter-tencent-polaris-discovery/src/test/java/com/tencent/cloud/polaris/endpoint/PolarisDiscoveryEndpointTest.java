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
package com.tencent.cloud.polaris.endpoint;

import java.util.Map;

import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClientConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for polaris discovery endpoint.
 *
 * @author shuiqingliu
 */
public class PolarisDiscoveryEndpointTest {

	private static NamingServer namingServer;

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisPropertiesConfiguration.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisDiscoveryAutoConfiguration.class,
					PolarisDiscoveryEndpointAutoConfiguration.class))
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues(
					"spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx");

	@BeforeClass
	public static void beforeClass() throws Exception {
		namingServer = NamingServer.startNamingServer(10081);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (null != namingServer) {
			namingServer.terminate();
		}
	}

	@Test
	public void testPolarisDiscoveryEndpoint() {
		this.contextRunner.run(context -> {
			PolarisDiscoveryProperties polarisDiscoveryProperties = context
					.getBean(PolarisDiscoveryProperties.class);
			DiscoveryClient discoveryClient = context
					.getBean(PolarisDiscoveryClient.class);
			PolarisDiscoveryHandler polarisDiscoveryHandler = context.getBean(PolarisDiscoveryHandler.class);
			PolarisDiscoveryEndpoint polarisDiscoveryEndpoint = new PolarisDiscoveryEndpoint(polarisDiscoveryProperties, discoveryClient, polarisDiscoveryHandler);

			Map<String, Object> mapInfo = polarisDiscoveryEndpoint.polarisDiscovery("java_provider_test");

			assertThat(polarisDiscoveryProperties).isEqualTo(mapInfo.get("PolarisDiscoveryProperties"));

		});
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableDiscoveryClient
	static class PolarisPropertiesConfiguration {

	}

}
