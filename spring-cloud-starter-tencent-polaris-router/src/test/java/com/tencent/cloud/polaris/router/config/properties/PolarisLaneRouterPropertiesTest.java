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

package com.tencent.cloud.polaris.router.config.properties;

import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.router.config.RouterConfigModifierAutoConfiguration;
import com.tencent.polaris.plugins.router.lane.BaseLaneMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * test for {@link PolarisLaneRouterProperties}.
 */
public class PolarisLaneRouterPropertiesTest {
	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					RouterConfigModifierAutoConfiguration.class
			))
			.withPropertyValues("spring.application.name=test");

	PolarisLaneRouterProperties properties;

	@BeforeEach
	void setUp() {
		properties = new PolarisLaneRouterProperties();
	}

	@Test
	public void testDefaultValues() {
		contextRunner.run(context -> {
			PolarisLaneRouterProperties props = context.getBean(PolarisLaneRouterProperties.class);
			assertThat(props.isEnabled()).isTrue();
			assertThat(props.getBaseLaneMode()).isEqualTo(BaseLaneMode.ONLY_UNTAGGED_INSTANCE);
		});
	}

	@Test
	public void testEnabledPropertyBinding() {
		contextRunner
				.withPropertyValues("spring.cloud.polaris.router.lane-router.enabled=false")
				.run(context -> {
					PolarisLaneRouterProperties props = context.getBean(PolarisLaneRouterProperties.class);
					assertThat(props.isEnabled()).isFalse();
				});
	}

	@Test
	public void testBaseLaneModePropertyBinding() {
		contextRunner
				.withPropertyValues("spring.cloud.polaris.router.lane-router.base-lane-mode=EXCLUDE_ENABLED_LANE_INSTANCE")
				.run(context -> {
					PolarisLaneRouterProperties props = context.getBean(PolarisLaneRouterProperties.class);
					assertThat(props.getBaseLaneMode()).isEqualTo(BaseLaneMode.EXCLUDE_ENABLED_LANE_INSTANCE);
				});
	}

	@Test
	public void testAllPropertiesBinding() {
		contextRunner
				.withPropertyValues(
						"spring.cloud.polaris.router.lane-router.enabled=false",
						"spring.cloud.polaris.router.lane-router.base-lane-mode=EXCLUDE_ENABLED_LANE_INSTANCE"
				)
				.run(context -> {
					PolarisLaneRouterProperties props = context.getBean(PolarisLaneRouterProperties.class);
					assertThat(props.isEnabled()).isFalse();
					assertThat(props.getBaseLaneMode()).isEqualTo(BaseLaneMode.EXCLUDE_ENABLED_LANE_INSTANCE);
				});
	}

	@Test
	public void testIsEnabled() {
		assertThat(properties.isEnabled()).isEqualTo(true);
	}

	@Test
	public void testSetEnabled() {
		properties.setEnabled(false);
		assertThat(properties.isEnabled()).isEqualTo(false);
	}

	@Test
	public void testGetBaseLaneMode() {
		assertThat(properties.getBaseLaneMode()).isEqualTo(BaseLaneMode.ONLY_UNTAGGED_INSTANCE);
	}

	@Test
	public void testSetBaseLaneMode() {
		properties.setBaseLaneMode(BaseLaneMode.EXCLUDE_ENABLED_LANE_INSTANCE);
		assertThat(properties.getBaseLaneMode()).isEqualTo(BaseLaneMode.EXCLUDE_ENABLED_LANE_INSTANCE);
	}

}
