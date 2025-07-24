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

package com.tencent.cloud.rpc.enhancement.stat.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisStatProperties}.
 *
 * @author Haotian Zhang
 */
public class PolarisStatPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PolarisStatPropertiesAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.path=/xxx")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.address=127.0.0.1:9091, 127.0.0.1:9092")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.namespace=test-namespace")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.service=test-service")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.push-interval=1000")
			.withPropertyValues("spring.cloud.gateway.enabled=false");

	@Test
	public void testDefaultInitialization() {
		contextRunner.run(context -> {
			PolarisStatProperties polarisStatProperties = context.getBean(PolarisStatProperties.class);
			PolarisStatPushGatewayProperties polarisStatPushGatewayProperties = context.getBean(PolarisStatPushGatewayProperties.class);
			assertThat(polarisStatProperties).isNotNull();
			assertThat(polarisStatProperties.isEnabled()).isTrue();
			assertThat(polarisStatProperties.getPath()).isEqualTo("/xxx");
			assertThat(polarisStatPushGatewayProperties.isPushGatewayEnabled()).isTrue();
			assertThat(polarisStatPushGatewayProperties.getAddress().get(0)).isEqualTo("127.0.0.1:9091");
			assertThat(polarisStatPushGatewayProperties.getAddress().get(1)).isEqualTo("127.0.0.1:9092");
			assertThat(polarisStatPushGatewayProperties.getStatNamespace()).isEqualTo("test-namespace");
			assertThat(polarisStatPushGatewayProperties.getStatService()).isEqualTo("test-service");
			assertThat(polarisStatPushGatewayProperties.getPushGatewayPushInterval().toString()).isEqualTo("1000");
		});
	}

	@Test
	void testGetAndSet() {
		PolarisStatProperties polarisStatProperties = new PolarisStatProperties();
		PolarisStatPushGatewayProperties polarisStatPushGatewayProperties = new PolarisStatPushGatewayProperties();
		// PushGatewayEnabled
		polarisStatPushGatewayProperties.setPushGatewayEnabled(true);
		assertThat(polarisStatPushGatewayProperties.isPushGatewayEnabled()).isTrue();

		// PushGatewayAddress
		List<String> pushGatewayAddress = List.of("127.0.0.1:9091", "127.0.0.1:9092");
		polarisStatPushGatewayProperties.setAddress(pushGatewayAddress);
		assertThat(polarisStatPushGatewayProperties.getAddress().get(0)).isEqualTo("127.0.0.1:9091");
		assertThat(polarisStatPushGatewayProperties.getAddress().get(1)).isEqualTo("127.0.0.1:9092");

		// PushGatewayPushInterval
		polarisStatPushGatewayProperties.setPushGatewayPushInterval(1000L);
		assertThat(polarisStatPushGatewayProperties.getPushGatewayPushInterval().toString()).isEqualTo("1000");
	}
}
