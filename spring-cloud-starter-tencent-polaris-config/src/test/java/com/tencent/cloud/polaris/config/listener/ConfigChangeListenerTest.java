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

package com.tencent.cloud.polaris.config.listener;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.tencent.cloud.polaris.config.adapter.PolarisConfigFileLocator;
import com.tencent.cloud.polaris.config.annotation.PolarisConfigKVFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.CompositePropertySource;
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
		properties = {"server.port=48081", "spring.config.location = classpath:application-test.yml",
				"spring.cloud.polaris.config.connect-remote-server=true", "spring.cloud.polaris.config.check-address=false", "spring.cloud.polaris.config.internal-enabled=false"
		})
public class ConfigChangeListenerTest {

	private static CountDownLatch hits = new CountDownLatch(2);
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private ConfigurableApplicationContext applicationContext;
	@Autowired
	private TestApplication.TestConfig testConfig;

	@BeforeAll
	public static void setUp() {
		try {
			Class<?> clazz = PolarisConfigFileLocator.class;
			Field field = clazz.getDeclaredField("compositePropertySourceCache");
			field.setAccessible(true);
			field.set(null, new CompositePropertySource("mock"));
		}
		catch (Exception e) {
			// ignore
		}
	}

	@Test
	public void test() throws InterruptedException {
		//before change
		Assertions.assertThat(testConfig.getTimeout()).isEqualTo(1000);
		Set<String> ketSet = new HashSet<>();
		ketSet.add("timeout");
		for (int i = 2; i <= 1000; i++) {
			//submit change event
			System.setProperty("timeout", String.valueOf(i * 1000));
			EnvironmentChangeEvent event = new EnvironmentChangeEvent(applicationContext, ketSet);
			applicationEventPublisher.publishEvent(event);
			//after change
			//Reset hits for each iteration
			boolean ret = hits.await(2, TimeUnit.SECONDS);
			Assertions.assertThat(ret).isEqualTo(true);
			hits = new CountDownLatch(2);
			Assertions.assertThat(testConfig.getChangeCnt()).isEqualTo(2 * i - 2);
			Assertions.assertThat(testConfig.getSyncChangeCnt()).isEqualTo(2 * i - 2);
			Assertions.assertThat(testConfig.getTimeout()).isEqualTo(i * 1000);
		}
	}

	@SpringBootApplication
	protected static class TestApplication {

		@Component
		protected static class TestConfig {

			@Value("${timeout:1000}")
			private int timeout;

			private AtomicInteger changeCnt = new AtomicInteger(0);

			private int syncChangeCnt;

			public int getTimeout() {
				return timeout;
			}

			public void setTimeout(int timeout) {
				this.timeout = timeout;
			}

			public int getChangeCnt() {
				return changeCnt.get();
			}

			public int getSyncChangeCnt() {
				return syncChangeCnt;
			}

			@PolarisConfigKVFileChangeListener(interestedKeys = {"timeout"})
			public void configChangedListener(ConfigChangeEvent event) {
				ConfigPropertyChangeInfo changeInfo = event.getChange("timeout");
				timeout = Integer.parseInt(changeInfo.getNewValue().toString());
				changeCnt.incrementAndGet();
				hits.countDown();
			}

			@PolarisConfigKVFileChangeListener(interestedKeyPrefixes = {"timeout"})
			public void configChangedListener2(ConfigChangeEvent event) {
				ConfigPropertyChangeInfo changeInfo = event.getChange("timeout");
				timeout = Integer.parseInt(changeInfo.getNewValue().toString());
				changeCnt.incrementAndGet();
				hits.countDown();
			}

			@PolarisConfigKVFileChangeListener(interestedKeys = {"timeout"}, async = false)
			public void syncConfigChangedListener(ConfigChangeEvent event) {
				ConfigPropertyChangeInfo changeInfo = event.getChange("timeout");
				timeout = Integer.parseInt(changeInfo.getNewValue().toString());
				syncChangeCnt++;
			}

			@PolarisConfigKVFileChangeListener(interestedKeyPrefixes = {"timeout"}, async = false)
			public void syncConfigChangedListener2(ConfigChangeEvent event) {
				ConfigPropertyChangeInfo changeInfo = event.getChange("timeout");
				timeout = Integer.parseInt(changeInfo.getNewValue().toString());
				syncChangeCnt++;
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
