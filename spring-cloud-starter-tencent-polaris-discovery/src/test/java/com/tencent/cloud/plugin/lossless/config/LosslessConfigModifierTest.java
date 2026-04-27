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

package com.tencent.cloud.plugin.lossless.config;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.config.provider.LosslessConfig;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link LosslessConfigModifier}.
 *
 * @author Shedfree Wu
 */
public class LosslessConfigModifierTest {

	private final ApplicationContextRunner delayRegisterContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=true")
			.withPropertyValues("spring.cloud.polaris.admin.port=20000")
			.withPropertyValues("spring.cloud.polaris.lossless.delayRegisterInterval=10")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.server.webflux.enabled=false");

	private final ApplicationContextRunner healthCheckContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=true")
			.withPropertyValues("spring.cloud.polaris.admin.port=20000")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckPath=/xxx")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckInterval=5")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.server.webflux.enabled=false");
	private final ApplicationContextRunner disabledContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=false")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.server.webflux.enabled=false");

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	void testDelayRegister() {
		delayRegisterContextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			LosslessConfig losslessConfig = polarisSDKContextManager.getSDKContext().
					getConfig().getProvider().getLossless();
			assertThat(losslessConfig.getDelayRegisterInterval()).isEqualTo(10);
			assertThat(losslessConfig.getStrategy()).isEqualTo(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_TIME);
		});
	}

	@Test
	void testHealthCheck() {
		healthCheckContextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			LosslessConfig losslessConfig = polarisSDKContextManager.getSDKContext().
					getConfig().getProvider().getLossless();
			assertThat(losslessConfig.getHealthCheckPath()).isEqualTo("/xxx");
			assertThat(losslessConfig.getHealthCheckInterval()).isEqualTo(5);
			assertThat(losslessConfig.getStrategy()).isEqualTo(LosslessProto.DelayRegister.DelayStrategy.DELAY_BY_HEALTH_CHECK);
		});
	}


	@Test
	void testDisabled() {
		disabledContextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			LosslessConfig losslessConfig = polarisSDKContextManager.getSDKContext().
					getConfig().getProvider().getLossless();
			assertThat(losslessConfig.isEnable()).isFalse();
		});
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
