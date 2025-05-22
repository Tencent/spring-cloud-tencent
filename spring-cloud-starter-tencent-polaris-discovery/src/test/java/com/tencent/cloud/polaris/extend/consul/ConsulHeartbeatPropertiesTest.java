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

package com.tencent.cloud.polaris.extend.consul;

import com.tencent.cloud.common.util.inet.PolarisInetUtilsAutoConfiguration;
import com.tencent.cloud.polaris.DiscoveryPropertiesAutoConfiguration;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConsulHeartbeatProperties}.
 *
 * @author Haotian Zhang
 */
public class ConsulHeartbeatPropertiesTest {

	@Test
	public void testGettersAndSetters() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisContextAutoConfiguration.class,
						PolarisInetUtilsAutoConfiguration.class, DiscoveryPropertiesAutoConfiguration.class))
				.withPropertyValues("spring.cloud.consul.enabled=true")
				.withPropertyValues("spring.cloud.consul.discovery.heartbeat.ttlValue=60")
				.withPropertyValues("spring.cloud.consul.discovery.heartbeat.ttl-unit=m")
				.withPropertyValues("spring.cloud.consul.discovery.heartbeat.interval-ratio=0.5");
		applicationContextRunner.run(context -> {
			assertThat(context).hasSingleBean(ConsulHeartbeatProperties.class);
			ConsulHeartbeatProperties heartbeatProperties = context.getBean(ConsulHeartbeatProperties.class);
			assertThat(heartbeatProperties.isEnabled()).isTrue();
			assertThat(heartbeatProperties.getTtlValue()).isEqualTo(60);
			assertThat(heartbeatProperties.getTtlUnit()).isEqualTo("m");
			assertThat(heartbeatProperties.getIntervalRatio()).isEqualTo(0.5);
		});
	}

	@Test
	public void testTtlValueWrong() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisInetUtilsAutoConfiguration.class, DiscoveryPropertiesAutoConfiguration.class))
				.withPropertyValues("spring.cloud.consul.enabled=true")
				.withPropertyValues("spring.cloud.consul.discovery.heartbeat.ttlValue=0");
		applicationContextRunner.run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasRootCauseExactlyInstanceOf(IllegalArgumentException.class)
					.hasRootCauseMessage("ttlValue must be at least 1");
		});
	}

	@Test
	public void testTtlUnitWrong() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisInetUtilsAutoConfiguration.class, DiscoveryPropertiesAutoConfiguration.class))
				.withPropertyValues("spring.cloud.consul.enabled=true")
				.withPropertyValues("spring.cloud.consul.discovery.heartbeat.ttl-unit=");
		applicationContextRunner.run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasRootCauseExactlyInstanceOf(IllegalArgumentException.class)
					.hasRootCauseMessage("ttlUnit cannot be null or empty");
		});
	}

	@Test
	public void testIntervalRatioWrong() {
		ApplicationContextRunner applicationContextRunner1 = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisInetUtilsAutoConfiguration.class, DiscoveryPropertiesAutoConfiguration.class))
				.withPropertyValues("spring.cloud.consul.enabled=true")
				.withPropertyValues("spring.cloud.consul.discovery.heartbeat.interval-ratio=0");
		applicationContextRunner1.run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasRootCauseExactlyInstanceOf(IllegalArgumentException.class)
					.hasRootCauseMessage("intervalRatio must be between 0.1 and 0.9");
		});

		ApplicationContextRunner applicationContextRunner2 = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisInetUtilsAutoConfiguration.class, DiscoveryPropertiesAutoConfiguration.class))
				.withPropertyValues("spring.cloud.consul.enabled=true")
				.withPropertyValues("spring.cloud.consul.discovery.heartbeat.interval-ratio=1");
		applicationContextRunner2.run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasRootCauseExactlyInstanceOf(IllegalArgumentException.class)
					.hasRootCauseMessage("intervalRatio must be between 0.1 and 0.9");
		});
	}
}
