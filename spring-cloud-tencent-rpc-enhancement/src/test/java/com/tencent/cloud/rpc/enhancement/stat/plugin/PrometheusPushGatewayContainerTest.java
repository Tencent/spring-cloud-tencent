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

package com.tencent.cloud.rpc.enhancement.stat.plugin;

import java.util.Objects;

import com.tencent.cloud.rpc.enhancement.stat.config.PolarisStatProperties;
import com.tencent.cloud.rpc.enhancement.stat.config.PolarisStatPropertiesAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.stat.config.plugin.PrometheusPushGatewayContainer;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PrometheusPushGatewayContainer}.
 *
 * @author lingxiao.wlx
 */
public class PrometheusPushGatewayContainerTest {

	@Test
	public void testWithPushGatewayAndPushGatewayEnabled() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.polaris.stat.pushgateway.enabled=true")
				.withPropertyValues("spring.cloud.polaris.stat.enabled=true")
				.withConfiguration(AutoConfigurations.of(PolarisStatPropertiesAutoConfiguration.class));
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(PrometheusPushGatewayContainer.class);
		});
	}

	@Test
	public void testWithoutPushGatewayEnabled() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.polaris.stat.pushgateway.enabled=false")
				.withPropertyValues("spring.cloud.polaris.stat.enabled=true")
				.withConfiguration(AutoConfigurations.of(PolarisStatPropertiesAutoConfiguration.class));
		contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(PrometheusPushGatewayContainer.class);
		});
	}

	@Test
	public void testWithoutStatEnabled() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.polaris.stat.pushgateway.enabled=true")
				.withPropertyValues("spring.cloud.polaris.stat.enabled=false")
				.withConfiguration(AutoConfigurations.of(PolarisStatPropertiesAutoConfiguration.class));
		contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(PrometheusPushGatewayContainer.class);
		});
	}

	@Test
	public void testWithoutPushGatewayAndStatEnabled() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.polaris.stat.pushgateway.enabled=false")
				.withPropertyValues("spring.cloud.polaris.stat.enabled=false")
				.withConfiguration(AutoConfigurations.of(PolarisStatPropertiesAutoConfiguration.class));
		contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(PrometheusPushGatewayContainer.class);
		});
	}

	@Test
	public void testPushGatewayProperties() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.polaris.stat.pushgateway.enabled=true")
				.withPropertyValues("spring.cloud.polaris.stat.enabled=true")
				.withPropertyValues("spring.cloud.polaris.stat.pushgateway.job=test")
				.withPropertyValues("spring.cloud.polaris.stat.pushgateway.grouping-keys.instance=test")
				.withPropertyValues("spring.cloud.polaris.stat.pushgateway.push-rate=1m")
				.withConfiguration(AutoConfigurations.of(PolarisStatPropertiesAutoConfiguration.class));
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(PrometheusPushGatewayContainer.class);
			PolarisStatProperties properties = context.getBean(PolarisStatProperties.class);
			PolarisStatProperties.PushGatewayProperties pushgateway = properties.getPushgateway();
			Assertions.assertFalse(Objects.isNull(pushgateway));
			Assertions.assertEquals(pushgateway.getJob(), "test");
			Assertions.assertEquals(pushgateway.getPushRate().toMillis(), 60000);
		});
	}
}
