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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisAsyncConfiguration}.
 *
 * @author Haotian Zhang
 */
public class PolarisAsyncConfigurationTest {

	private final ApplicationContextRunner contextRunnerWithoutPolarisAsyncConfiguration
			= new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PolarisAsyncConfiguration.class));

	private final ApplicationContextRunner contextRunnerWithPolarisAsyncConfiguration
			= new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PolarisAsyncConfiguration.class, TestConfiguration.class))
			.withPropertyValues("spring.cloud.tencent.async.enabled=true");

	@Test
	public void testAsyncExecutor() {
		this.contextRunnerWithoutPolarisAsyncConfiguration.run(context -> {
			assertThat(context).doesNotHaveBean(PolarisAsyncConfiguration.class);
		});

		this.contextRunnerWithPolarisAsyncConfiguration.run(context -> {
			assertThat(context).hasSingleBean(PolarisAsyncConfiguration.class);
			assertThat(context).hasSingleBean(TaskExecutor.class);
			String[] executorNames = context.getBeanNamesForType(TaskExecutor.class);
			List<String> executorNameList = Arrays.asList(executorNames);
			assertThat(executorNameList).contains("polarisAsyncExecutor");
		});
	}

	@Configuration
	@Async
	protected static class TestConfiguration {

	}
}
