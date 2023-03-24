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

package com.tencent.cloud.common.metadata.config;


import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test for {@link MetadataAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
public class MetadataAutoConfigurationTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner().withPropertyValues(
			"spring.application.name=test"
	);

	private final WebApplicationContextRunner webApplicationContextRunner = new WebApplicationContextRunner().withPropertyValues(
			"spring.application.name=test"
	);

	private final ReactiveWebApplicationContextRunner reactiveWebApplicationContextRunner = new ReactiveWebApplicationContextRunner().withPropertyValues(
			"spring.application.name=test"
	);

	/**
	 * No any web application.
	 */
	@Test
	public void test1() {
		this.applicationContextRunner
				.withConfiguration(AutoConfigurations.of(MetadataAutoConfiguration.class, ApplicationContextAwareUtils.class))
				.run(context -> {
					Assertions.assertThat(context).hasSingleBean(MetadataLocalProperties.class);
				});
	}

	/**
	 * web application.
	 */
	@Test
	public void test2() {
		this.webApplicationContextRunner
				.withConfiguration(AutoConfigurations.of(MetadataAutoConfiguration.class, ApplicationContextAwareUtils.class))
				.run(context -> {
					Assertions.assertThat(context).hasSingleBean(MetadataLocalProperties.class);
				});
	}

	/**
	 * reactive web application.
	 */
	@Test
	public void test3() {
		this.reactiveWebApplicationContextRunner
				.withConfiguration(AutoConfigurations.of(MetadataAutoConfiguration.class, ApplicationContextAwareUtils.class))
				.run(context -> {
					Assertions.assertThat(context).hasSingleBean(MetadataLocalProperties.class);
				});
	}
}
