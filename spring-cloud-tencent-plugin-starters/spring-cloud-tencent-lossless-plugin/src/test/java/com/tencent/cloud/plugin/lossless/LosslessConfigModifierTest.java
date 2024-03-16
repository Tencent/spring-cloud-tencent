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

package com.tencent.cloud.plugin.lossless;

import com.tencent.cloud.plugin.lossless.config.LosslessConfigModifier;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.config.provider.LosslessConfig;
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

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.port=20000")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckPath=/xxx")
			.withPropertyValues("spring.cloud.polaris.lossless.delayRegisterInterval=10")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckInterval=5")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.enabled=false");
	private final ApplicationContextRunner disabledContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=false")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.enabled=false");

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	void testModify() {
		contextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			LosslessConfig losslessConfig = polarisSDKContextManager.getSDKContext().
					getConfig().getProvider().getLossless();
			assertThat(losslessConfig.getHost()).isEqualTo("0.0.0.0");
			assertThat(losslessConfig.getPort()).isEqualTo(20000);
			assertThat(losslessConfig.getDelayRegisterInterval()).isEqualTo(10);
			assertThat(losslessConfig.getHealthCheckInterval()).isEqualTo(5);
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
