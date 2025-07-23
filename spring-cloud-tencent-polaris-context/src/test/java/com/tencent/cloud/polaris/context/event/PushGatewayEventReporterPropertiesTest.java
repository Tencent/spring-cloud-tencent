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

package com.tencent.cloud.polaris.context.event;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PushGatewayEventReporterProperties}.
 *
 * @author Haotian Zhang
 */
public class PushGatewayEventReporterPropertiesTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PolarisContextAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.event.pushgateway.enabled=true")
			.withPropertyValues("spring.cloud.polaris.event.pushgateway.address=1.2.3.4:9091,1.2.3.5:9091")
			.withPropertyValues("spring.cloud.polaris.event.pushgateway.eventQueueSize=123")
			.withPropertyValues("spring.cloud.polaris.event.pushgateway.maxBatchSize=456")
			.withPropertyValues("spring.cloud.polaris.event.pushgateway.namespace=test-namespace")
			.withPropertyValues("spring.cloud.polaris.event.pushgateway.service=test-service");

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	public void testGetAndSet() {
		this.applicationContextRunner.run(context -> {
			PushGatewayEventReporterProperties properties = context.getBean(PushGatewayEventReporterProperties.class);
			assertThat(properties.isEnabled()).isTrue();
			assertThat(properties.getAddress().get(0)).isEqualTo("1.2.3.4:9091");
			assertThat(properties.getAddress().get(1)).isEqualTo("1.2.3.5:9091");
			assertThat(properties.getEventQueueSize()).isEqualTo(123);
			assertThat(properties.getMaxBatchSize()).isEqualTo(456);
			assertThat(properties.getNamespace()).isEqualTo("test-namespace");
			assertThat(properties.getService()).isEqualTo("test-service");
		});
	}
}
