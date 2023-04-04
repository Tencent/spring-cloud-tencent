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

package com.tencent.cloud.rpc.enhancement.stat.config;

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
			.withPropertyValues("spring.cloud.polaris.stat.host=127.0.0.1")
			.withPropertyValues("spring.cloud.polaris.stat.port=20000")
			.withPropertyValues("spring.cloud.polaris.stat.path=/xxx")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.address=127.0.0.1:9091")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.push-interval=1000")
			.withPropertyValues("spring.cloud.gateway.enabled=false");

	@Test
	public void testDefaultInitialization() {
		contextRunner.run(context -> {
			PolarisStatProperties polarisStatProperties = context.getBean(PolarisStatProperties.class);

			assertThat(polarisStatProperties).isNotNull();
			assertThat(polarisStatProperties.isEnabled()).isTrue();
			assertThat(polarisStatProperties.getHost()).isNotBlank();
			assertThat(polarisStatProperties.getPort()).isEqualTo(20000);
			assertThat(polarisStatProperties.getPath()).isEqualTo("/xxx");
			assertThat(polarisStatProperties.isPushGatewayEnabled()).isTrue();
			assertThat(polarisStatProperties.getPushGatewayAddress()).isEqualTo("127.0.0.1:9091");
			assertThat(polarisStatProperties.getPushGatewayPushInterval().toString()).isEqualTo("1000");
		});
	}

	@Test
	void testGetAndSet() {
		PolarisStatProperties polarisStatProperties = new PolarisStatProperties();

		// PushGatewayEnabled
		polarisStatProperties.setPushGatewayEnabled(true);
		assertThat(polarisStatProperties.isPushGatewayEnabled()).isTrue();

		// PushGatewayAddress
		polarisStatProperties.setPushGatewayAddress("127.0.0.1:9091");
		assertThat(polarisStatProperties.getPushGatewayAddress()).isEqualTo("127.0.0.1:9091");

		// PushGatewayPushInterval
		polarisStatProperties.setPushGatewayPushInterval(1000L);
		assertThat(polarisStatProperties.getPushGatewayPushInterval().toString()).isEqualTo("1000");
	}
}
