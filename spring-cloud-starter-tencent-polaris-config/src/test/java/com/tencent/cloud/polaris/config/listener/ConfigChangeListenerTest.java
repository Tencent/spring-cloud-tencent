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

package com.tencent.cloud.polaris.config.listener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.tencent.cloud.polaris.config.annotation.PolarisConfigKVFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

/**
 * Integration testing for change listener.
 *
 * @author lepdou 2022-06-11
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = DEFINED_PORT, classes = ConfigChangeListenerTest.TestApplication.class,
		properties = {"server.port=48081", "spring.config.location = classpath:application-test.yml"})
public class ConfigChangeListenerTest {

	private static final CountDownLatch hits = new CountDownLatch(2);
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private ConfigurableApplicationContext applicationContext;
	@Autowired
	private TestApplication.TestConfig testConfig;

	@Test
	public void test() throws InterruptedException {
		//before change
		Assertions.assertThat(testConfig.getTimeout()).isEqualTo(1000);

		//submit change event
		System.setProperty("timeout", "2000");
		EnvironmentChangeEvent event = new EnvironmentChangeEvent(applicationContext,
				Sets.newHashSet("timeout"));

		applicationEventPublisher.publishEvent(event);

		//after change
		boolean ret = hits.await(2, TimeUnit.SECONDS);
		Assertions.assertThat(ret).isEqualTo(true);

		Assertions.assertThat(testConfig.getChangeCnt()).isEqualTo(2);
		Assertions.assertThat(testConfig.getTimeout()).isEqualTo(2000);
	}

	@SpringBootApplication
	protected static class TestApplication {

		@Component
		protected static class TestConfig {

			@Value("${timeout:1000}")
			private int timeout;

			private int changeCnt;

			public int getTimeout() {
				return timeout;
			}

			public void setTimeout(int timeout) {
				this.timeout = timeout;
			}

			public int getChangeCnt() {
				return changeCnt;
			}

			@PolarisConfigKVFileChangeListener(interestedKeys = {"timeout"})
			public void configChangedListener(ConfigChangeEvent event) {
				ConfigPropertyChangeInfo changeInfo = event.getChange("timeout");
				timeout = Integer.parseInt(changeInfo.getNewValue().toString());
				changeCnt++;
				hits.countDown();
			}

			@PolarisConfigKVFileChangeListener(interestedKeyPrefixes = {"timeout"})
			public void configChangedListener2(ConfigChangeEvent event) {
				ConfigPropertyChangeInfo changeInfo = event.getChange("timeout");
				timeout = Integer.parseInt(changeInfo.getNewValue().toString());
				changeCnt++;
				hits.countDown();
			}
		}

		@Component
		protected static class EventPublisher implements ApplicationEventPublisher {

			@Override
			public void publishEvent(Object o) {

			}
		}
	}
}
