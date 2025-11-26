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

package com.tencent.cloud.plugin.kafka;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link KafkaLaneProperties}.
 */
public class KafkaLanePropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	public void testDefaultValues() {
		this.contextRunner.run(context -> {
			KafkaLaneProperties properties = context.getBean(KafkaLaneProperties.class);

			// test default values
			assertThat(properties.getLaneOn()).isFalse();
			assertThat(properties.getLaneConsumeMain()).isFalse();
			assertThat(properties.getMainConsumeLane()).isFalse();
		});
	}

	@Test
	public void testConfigurationPropertiesPrefix() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.polaris.lane.kafka.lane-on=true",
						"spring.cloud.polaris.lane.kafka.lane-consume-main=true",
						"spring.cloud.polaris.lane.kafka.main-consume-lane=true"
				)
				.run(context -> {
					KafkaLaneProperties properties = context.getBean(KafkaLaneProperties.class);

					// test properties
					assertThat(properties.getLaneOn()).isTrue();
					assertThat(properties.getLaneConsumeMain()).isTrue();
					assertThat(properties.getMainConsumeLane()).isTrue();
				});
	}

	@Test
	public void testGetterAndSetter() {
		KafkaLaneProperties properties = new KafkaLaneProperties();

		// test laneOn property
		properties.setLaneOn(true);
		assertThat(properties.getLaneOn()).isTrue();
		properties.setLaneOn(false);
		assertThat(properties.getLaneOn()).isFalse();

		// test laneConsumeMain property
		properties.setLaneConsumeMain(true);
		assertThat(properties.getLaneConsumeMain()).isTrue();
		properties.setLaneConsumeMain(false);
		assertThat(properties.getLaneConsumeMain()).isFalse();

		// test mainConsumeLane property
		properties.setMainConsumeLane(true);
		assertThat(properties.getMainConsumeLane()).isTrue();
		properties.setMainConsumeLane(false);
		assertThat(properties.getMainConsumeLane()).isFalse();
	}

	@Test
	public void testPartialConfiguration() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.polaris.lane.kafka.lane-on=true"
				)
				.run(context -> {
					KafkaLaneProperties properties = context.getBean(KafkaLaneProperties.class);

					// test laneOn property
					assertThat(properties.getLaneOn()).isTrue();
					// test other properties keep default values
					assertThat(properties.getLaneConsumeMain()).isFalse();
					assertThat(properties.getMainConsumeLane()).isFalse();
				});
	}

	@EnableConfigurationProperties(KafkaLaneProperties.class)
	static class TestConfiguration {
	}
}
