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

package com.tencent.cloud.common.util;

import com.tencent.cloud.common.async.PolarisAsyncConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link ApplicationContextAwareUtils}.
 *
 * @author Haotian Zhang
 */
public class ApplicationContextAwareUtilsTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer(new ApplicationContextAwareUtils())
			.withUserConfiguration(TestConfig.class)
			.withPropertyValues("key1=value1");

	@Test
	public void testApplicationContextAwareUtils() {
		this.contextRunner.run(context -> {
			assertThat(ApplicationContextAwareUtils.getApplicationContext()).isNotNull();

			// test getProperties
			assertThat(ApplicationContextAwareUtils.getProperties("key1")).isEqualTo("value1");
			assertThat(ApplicationContextAwareUtils.getProperties("key2")).isNull();

			// test getProperties with default value
			assertThat(ApplicationContextAwareUtils.getProperties("key1", "defaultValue")).isEqualTo("value1");
			assertThat(ApplicationContextAwareUtils.getProperties("key2", "defaultValue")).isEqualTo("defaultValue");

			// test getBean
			assertThat(ApplicationContextAwareUtils.getBean(TestConfig.class)).isNotNull();
			assertThatThrownBy(() -> {
				ApplicationContextAwareUtils.getBean(PolarisAsyncConfiguration.class);
			}).isInstanceOf(NoSuchBeanDefinitionException.class);

			// test getBeanIfExists
			assertThat(ApplicationContextAwareUtils.getBeanIfExists(TestConfig.class)).isNotNull();
			assertThat(ApplicationContextAwareUtils.getBeanIfExists(PolarisAsyncConfiguration.class, true)).isNull();
		});
	}

	@Configuration
	static class TestConfig {

	}
}
