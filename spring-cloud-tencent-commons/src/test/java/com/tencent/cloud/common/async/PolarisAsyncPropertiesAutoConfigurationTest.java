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

package com.tencent.cloud.common.async;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test for {@link PolarisAsyncPropertiesAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
public class PolarisAsyncPropertiesAutoConfigurationTest {

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
				.withConfiguration(AutoConfigurations.of(PolarisAsyncPropertiesAutoConfiguration.class))
				.run(context -> {
					Assertions.assertThat(context).hasSingleBean(PolarisAsyncProperties.class);
				});
	}

	/**
	 * web application.
	 */
	@Test
	public void test2() {
		this.webApplicationContextRunner
				.withConfiguration(AutoConfigurations.of(PolarisAsyncPropertiesAutoConfiguration.class))
				.run(context -> {
					Assertions.assertThat(context).hasSingleBean(PolarisAsyncProperties.class);
				});
	}

	/**
	 * reactive web application.
	 */
	@Test
	public void test3() {
		this.reactiveWebApplicationContextRunner
				.withConfiguration(AutoConfigurations.of(PolarisAsyncPropertiesAutoConfiguration.class))
				.run(context -> {
					Assertions.assertThat(context).hasSingleBean(PolarisAsyncProperties.class);
				});
	}
}
