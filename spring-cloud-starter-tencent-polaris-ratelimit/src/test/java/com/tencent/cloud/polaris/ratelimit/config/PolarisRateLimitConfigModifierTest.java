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

package com.tencent.cloud.polaris.ratelimit.config;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.config.provider.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class PolarisRateLimitConfigModifierTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.polaris.ratelimit.maxQueuingTime=500")
			.withPropertyValues("spring.cloud.polaris.ratelimit.limiterAddresses=127.0.0.1:8080,127.0.0.1:8081")
			.withPropertyValues("spring.cloud.polaris.ratelimit.remoteTaskInterval=50");

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	public void testModify() {
		this.applicationContextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			RateLimitConfig config = polarisSDKContextManager.getSDKContext().
					getConfig().getProvider().getRateLimit();
			assertThat(config.isEnable()).isTrue();
			assertThat(config.getLimiterAddresses().get(0)).isEqualTo("127.0.0.1:8080");
			assertThat(config.getLimiterAddresses().get(1)).isEqualTo("127.0.0.1:8081");
			assertThat(config.getMaxQueuingTime()).isEqualTo(500);
			assertThat(config.getRemoteTaskIntervalMilli()).isEqualTo(50);
		});
	}


	@SpringBootApplication
	protected static class TestApplication {

	}
}
