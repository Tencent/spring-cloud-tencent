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

import com.google.common.collect.Sets;
import com.tencent.cloud.polaris.config.annotation.PolarisConfigKVFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

/**
 * Integration testing for change listener.
 *@author lepdou 2022-06-11
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = DEFINED_PORT,
		classes = ConfigChangeListenerTest.TestApplication.class,
		properties = {"server.port=8081",
				"spring.config.location = classpath:application-test.yml"})
public class ConfigChangeListenerTest {

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private ConfigurableApplicationContext applicationContext;
	@Autowired
	private TestApplication.TestConfig testConfig;

	@Test
	public void test() throws InterruptedException {
		//before change
		Assert.assertEquals(1000, testConfig.getTimeout());

		//submit change event
		System.setProperty("timeout", "2000");
		EnvironmentChangeEvent event = new EnvironmentChangeEvent(applicationContext,
				Sets.newHashSet("timeout"));

		applicationEventPublisher.publishEvent(event);
		Thread.sleep(200);
		//after change
		Assert.assertEquals(2, testConfig.getChangeCnt());
		Assert.assertEquals(2000, testConfig.getTimeout());
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
				timeout = Integer.parseInt(changeInfo.getNewValue());
				changeCnt++;
			}

			@PolarisConfigKVFileChangeListener(interestedKeyPrefixes = {"timeout"})
			public void configChangedListener2(ConfigChangeEvent event) {
				ConfigPropertyChangeInfo changeInfo = event.getChange("timeout");
				timeout = Integer.parseInt(changeInfo.getNewValue());
				changeCnt++;
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
