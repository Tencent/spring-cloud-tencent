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

package com.tencent.cloud.polaris.context.event;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.plugins.event.pushgateway.PushGatewayEventReporterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PushGatewayEventReporterConfigModifier}.
 *
 * @author Haotian Zhang
 */
public class PushGatewayEventReporterConfigModifierTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PolarisContextAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.event.pushgateway.enabled=true")
			.withPropertyValues("spring.cloud.polaris.event.pushgateway.address=1.2.3.4:9091")
			.withPropertyValues("spring.cloud.polaris.event.pushgateway.eventQueueSize=123")
			.withPropertyValues("spring.cloud.polaris.event.pushgateway.maxBatchSize=456");

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	public void testModify() {
		this.applicationContextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			PushGatewayEventReporterConfig config = polarisSDKContextManager.getSDKContext().
					getConfig().getGlobal().getEventReporter()
					.getPluginConfig(DefaultPlugins.PUSH_GATEWAY_EVENT_REPORTER_TYPE, PushGatewayEventReporterConfig.class);
			assertThat(config.isEnable()).isTrue();
			assertThat(config.getAddress()).isEqualTo("1.2.3.4:9091");
			assertThat(config.getEventQueueSize()).isEqualTo(123);
			assertThat(config.getMaxBatchSize()).isEqualTo(456);
		});
	}
}
