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

package com.tencent.cloud.polaris.circuitbreaker.feign;

import com.tencent.cloud.polaris.circuitbreaker.PolarisFeignClientAutoConfiguration;
import com.tencent.cloud.polaris.context.PolarisContextConfiguration;
import feign.Client;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test for {@link PolarisFeignClient}.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestPolarisFeignApp.class)
@ContextConfiguration(classes = { PolarisFeignClientAutoConfiguration.class, PolarisContextConfiguration.class })
public class PolarisFeignClientTest {

	@Autowired
	private ApplicationContext springCtx;

	@Test
	public void testPolarisFeignBeanPostProcessor() {
		final PolarisFeignBeanPostProcessor postProcessor = springCtx.getBean(PolarisFeignBeanPostProcessor.class);
		Assertions.assertNotNull(postProcessor, "PolarisFeignBeanPostProcessor");
	}

	@Test
	public void testFeignClient() {
		final Client client = springCtx.getBean(Client.class);
		if (client instanceof PolarisFeignClient) {
			return;
		}
		if (client instanceof PolarisFeignBlockingLoadBalancerClient) {
			return;
		}
		throw new IllegalStateException("Polaris burying failed");
	}

}
