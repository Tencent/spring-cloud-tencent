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

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandlerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static com.tencent.polaris.api.config.global.StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link StatConfigModifier}.
 *
 * @author Haotian Zhang
 */
public class StatConfigModifierTest {

	private final ApplicationContextRunner pullContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.host=127.0.0.1")
			.withPropertyValues("spring.cloud.polaris.stat.port=20000")
			.withPropertyValues("spring.cloud.polaris.stat.path=/xxx")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.enabled=false");

	private final ApplicationContextRunner pushContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.address=127.0.0.1:9091")
			.withPropertyValues("spring.cloud.polaris.stat.pushgateway.push-interval=1000")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.enabled=false");

	private final ApplicationContextRunner disabledContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.cloud.polaris.stat.enabled=false")
			.withPropertyValues("spring.application.name=test")
			.withPropertyValues("spring.cloud.gateway.enabled=false");

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	void testPull() {
		pullContextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			PrometheusHandlerConfig prometheusHandlerConfig = polarisSDKContextManager.getSDKContext().getConfig()
					.getGlobal().getStatReporter()
					.getPluginConfig(DEFAULT_REPORTER_PROMETHEUS, PrometheusHandlerConfig.class);
			assertThat(prometheusHandlerConfig.getType()).isEqualTo("pull");
			assertThat(prometheusHandlerConfig.getHost()).isEqualTo("127.0.0.1");
			assertThat(prometheusHandlerConfig.getPort()).isEqualTo(20000);
			assertThat(prometheusHandlerConfig.getPath()).isEqualTo("/xxx");
		});
	}

	@Test
	void testPush() {
		pushContextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			PrometheusHandlerConfig prometheusHandlerConfig = polarisSDKContextManager.getSDKContext().getConfig()
					.getGlobal().getStatReporter()
					.getPluginConfig(DEFAULT_REPORTER_PROMETHEUS, PrometheusHandlerConfig.class);
			assertThat(prometheusHandlerConfig.getType()).isEqualTo("push");
			assertThat(prometheusHandlerConfig.getAddress()).isEqualTo("127.0.0.1:9091");
			assertThat(prometheusHandlerConfig.getPushInterval()).isEqualTo(1000);
		});
	}

	@Test
	void testDisabled() {
		disabledContextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			PrometheusHandlerConfig prometheusHandlerConfig = polarisSDKContextManager.getSDKContext().getConfig()
					.getGlobal().getStatReporter()
					.getPluginConfig(DEFAULT_REPORTER_PROMETHEUS, PrometheusHandlerConfig.class);
			assertThat(prometheusHandlerConfig.getPort()).isEqualTo(-1);
		});
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
