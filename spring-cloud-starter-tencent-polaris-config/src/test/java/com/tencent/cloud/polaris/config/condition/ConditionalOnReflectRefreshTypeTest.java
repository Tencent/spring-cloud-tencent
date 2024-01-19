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
 *
 */

package com.tencent.cloud.polaris.config.condition;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Objects;

import com.tencent.cloud.polaris.config.PolarisConfigAutoConfiguration;
import com.tencent.cloud.polaris.config.PolarisConfigBootstrapAutoConfiguration;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.adapter.PolarisRefreshAffectedContextRefresher;
import com.tencent.cloud.polaris.config.adapter.PolarisRefreshEntireContextRefresher;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.enums.RefreshType;
import com.tencent.cloud.polaris.config.spring.annotation.SpringValueProcessor;
import com.tencent.cloud.polaris.config.spring.property.PlaceholderHelper;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.refresh.ContextRefresher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConditionalOnReflectRefreshType}.
 *
 * @author lingxiao.wlx
 */
public class ConditionalOnReflectRefreshTypeTest {

	private static ServerSocket serverSocket;

	@BeforeAll
	static void beforeAll() {
		new Thread(() -> {
			try {
				serverSocket = new ServerSocket(8093);
				serverSocket.accept();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	@AfterAll
	static void afterAll() throws IOException {
		if (Objects.nonNull(serverSocket)) {
			serverSocket.close();
		}
	}

	@Test
	public void testReflectEnabled() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisConfigBootstrapAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(PolarisConfigAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(ConfigurationPropertiesRebinderAutoConfiguration.class))
				.withPropertyValues("spring.application.name=" + "conditionalOnConfigReflectEnabledTest")
				.withPropertyValues("server.port=" + 8080)
				.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
				.withPropertyValues("spring.cloud.polaris.config.refresh-type=" + RefreshType.REFLECT)
				.withPropertyValues("spring.cloud.polaris.config.enabled=true");
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(PlaceholderHelper.class);
			assertThat(context).hasSingleBean(SpringValueRegistry.class);
			assertThat(context).hasSingleBean(SpringValueProcessor.class);
			assertThat(context).hasSingleBean(PolarisRefreshAffectedContextRefresher.class);
		});
	}

	@Test
	public void testWithoutReflectEnabled() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisConfigBootstrapAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(PolarisConfigAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(ConfigurationPropertiesRebinderAutoConfiguration.class))
				.withPropertyValues("spring.application.name=" + "conditionalOnConfigReflectEnabledTest")
				.withPropertyValues("server.port=" + 8080)
				.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
				.withPropertyValues("spring.cloud.polaris.config.refresh-type=" + RefreshType.REFRESH_CONTEXT)
				.withPropertyValues("spring.cloud.polaris.config.enabled=true");
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(PolarisConfigProperties.class);
			assertThat(context).hasSingleBean(PolarisPropertySourceManager.class);
			assertThat(context).hasSingleBean(ContextRefresher.class);
			assertThat(context).hasSingleBean(PolarisRefreshEntireContextRefresher.class);
		});
	}
}
