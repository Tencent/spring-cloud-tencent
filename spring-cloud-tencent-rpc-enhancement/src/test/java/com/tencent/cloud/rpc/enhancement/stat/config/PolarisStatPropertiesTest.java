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

import com.tencent.cloud.common.util.AddressUtils;
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
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.address=127.0.0.1:9091,  127.0.0.1:9092")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.namespace=test-namespace")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.service=test-service")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.push-interval=1000")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.override-host=127.0.0.1")
			.withPropertyValues("spring.cloud.gateway.enabled=false");

	@Test
	public void testDefaultInitialization() {
		contextRunner.run(context -> {
			PolarisStatProperties polarisStatProperties = context.getBean(PolarisStatProperties.class);

			assertThat(polarisStatProperties).isNotNull();
			assertThat(polarisStatProperties.isEnabled()).isTrue();
			assertThat(polarisStatProperties.getPath()).isEqualTo("/xxx");
			assertThat(polarisStatProperties.isPushGatewayEnabled()).isTrue();
			List<String> addresses = AddressUtils.parseHostPortList(polarisStatProperties.getPushGatewayAddress());
			assertThat(addresses.get(0)).isEqualTo("127.0.0.1:9091");
			assertThat(addresses.get(1)).isEqualTo("127.0.0.1:9092");
			assertThat(polarisStatProperties.getStatNamespace()).isEqualTo("test-namespace");
			assertThat(polarisStatProperties.getStatService()).isEqualTo("test-service");
			assertThat(polarisStatProperties.getPushGatewayPushInterval().toString()).isEqualTo("1000");
			assertThat(polarisStatProperties.getPushGatewayOverrideHost()).isEqualTo("127.0.0.1");
		});
	}

	@Test
	void testGetAndSet() {
		PolarisStatProperties polarisStatProperties = new PolarisStatProperties();

		// PushGatewayEnabled
		polarisStatProperties.setPushGatewayEnabled(true);
		assertThat(polarisStatProperties.isPushGatewayEnabled()).isTrue();

		// PushGatewayAddress
		String pushGatewayAddress = "127.0.0.1:9091, " + "127.0.0.1:9092";
		polarisStatProperties.setPushGatewayAddress(pushGatewayAddress);
		List<String> addresses = AddressUtils.parseHostPortList(polarisStatProperties.getPushGatewayAddress());
		assertThat(addresses.get(0)).isEqualTo("127.0.0.1:9091");
		assertThat(addresses.get(1)).isEqualTo("127.0.0.1:9092");

		// PushGatewayPushInterval
		polarisStatProperties.setPushGatewayPushInterval(1000L);
		assertThat(polarisStatProperties.getPushGatewayPushInterval().toString()).isEqualTo("1000");
	}
}
