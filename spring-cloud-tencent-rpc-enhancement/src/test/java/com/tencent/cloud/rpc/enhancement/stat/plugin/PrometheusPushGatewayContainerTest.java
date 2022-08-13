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
import com.tencent.cloud.rpc.enhancement.stat.config.plugin.PrometheusPushGatewayContainer;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test for {@link PrometheusPushGatewayContainer}.
 *
 * @author lingxiao.wlx
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = PrometheusPushGatewayContainerTest.TestApplication.class,
		properties = {"spring.cloud.polaris.stat.pushgateway.enabled=true",
				"spring.cloud.polaris.stat.enabled=true",
				"spring.cloud.polaris.stat.pushgateway.shut-down-strategy=DELETE",
				"spring.cloud.polaris.stat.pushgateway.push-rate=1m",
				"spring.cloud.polaris.stat.pushgateway.job=test",
				"spring.cloud.polaris.stat.pushgateway.grouping-keys.instance=test"})
public class PrometheusPushGatewayContainerTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void prometheusPushGatewayContainerTest() {
		PolarisStatProperties polarisStatProperties = applicationContext.getBean(PolarisStatProperties.class);
		PolarisStatProperties.PushGatewayProperties pushgateway = polarisStatProperties.getPushgateway();
		Assertions.assertFalse(Objects.isNull(pushgateway));
		Assertions.assertEquals(pushgateway.getJob(), "test");
		Assertions.assertEquals(pushgateway.getPushRate().toMillis(), 60000);
		Assertions.assertEquals(pushgateway.getShutDownStrategy(), PolarisStatProperties.ShutDownStrategy.DELETE);
		applicationContext.getBean(PrometheusPushGatewayContainer.class);
	}

	@SpringBootApplication
	protected static class TestApplication {
	}
}
