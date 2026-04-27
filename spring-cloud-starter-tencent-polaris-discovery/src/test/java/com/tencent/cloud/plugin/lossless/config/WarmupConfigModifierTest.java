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
import com.tencent.polaris.api.config.consumer.WeightAdjustConfig;
import com.tencent.polaris.plugin.lossless.warmup.WarmupWeightAdjuster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link WarmupConfigModifier}.
 *
 * @author Shedfree Wu
 */
public class WarmupConfigModifierTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.warmup.enabled=true")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.server.webflux.enabled=false");
	private final ApplicationContextRunner disabledContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.warmup.enabled=false")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.server.webflux.enabled=false");

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	void testModify() {
		contextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			WeightAdjustConfig weightAdjustConfig = polarisSDKContextManager.getSDKContext().
					getConfig().getConsumer().getWeightAdjust();
			assertThat(weightAdjustConfig.isEnable()).isTrue();
			assertThat(weightAdjustConfig.getChain().contains(WarmupWeightAdjuster.WARMUP_WEIGHT_ADJUSTER_NAME)).isTrue();
		});
	}


	@Test
	void testDisabled() {
		disabledContextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			WeightAdjustConfig weightAdjustConfig = polarisSDKContextManager.getSDKContext().
					getConfig().getConsumer().getWeightAdjust();
			assertThat(weightAdjustConfig.isEnable()).isTrue();
			assertThat(weightAdjustConfig.getChain().contains(WarmupWeightAdjuster.WARMUP_WEIGHT_ADJUSTER_NAME)).isFalse();
		});
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
