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

package com.tencent.cloud.polaris.config.spring.event;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.tencent.polaris.configuration.api.core.ChangeType;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;


/**
 * Test for {@link ConfigChangeSpringEvent}.
 *
 * @author derek.yi 2022-10-16
 */
public class ConfigChangeSpringEventTest {

	private static CountDownLatch countDownLatch = new CountDownLatch(1);

	private static AtomicInteger receiveEventTimes = new AtomicInteger();

	@Test
	public void testPublishConfigChangeSpringEvent() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(ConfigChangeSpringEventListener.class));
		contextRunner.run(context -> {
			HashMap<String, ConfigPropertyChangeInfo> changeMap = new HashMap<>();
			changeMap.put("key", new ConfigPropertyChangeInfo("key", null, "value", ChangeType.ADDED));
			context.publishEvent(new ConfigChangeSpringEvent(changeMap));
			countDownLatch.await(5, TimeUnit.SECONDS);
			Assert.assertEquals(1, receiveEventTimes.get());
		});
	}

	@Configuration
	static class ConfigChangeSpringEventListener implements ApplicationListener<ConfigChangeSpringEvent> {

		@Override
		public void onApplicationEvent(ConfigChangeSpringEvent event) {
			Set<String> changedKeys = event.changedKeys();
			Assert.assertEquals(1, changedKeys.size());
			Assert.assertTrue(event.isChanged("key"));
			ConfigPropertyChangeInfo changeInfo = event.getChange("key");
			Assert.assertNotNull(changeInfo);
			Assert.assertEquals("key", changeInfo.getPropertyName());
			Assert.assertEquals("value", changeInfo.getNewValue());
			Assert.assertEquals(ChangeType.ADDED, changeInfo.getChangeType());

			receiveEventTimes.incrementAndGet();
			countDownLatch.countDown();
		}
	}
}
