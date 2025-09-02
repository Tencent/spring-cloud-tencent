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

package org.springframework.tsf.core.util;

import com.tencent.cloud.common.async.PolarisAsyncConfiguration;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link TsfSpringContextAware}.
 *
 * @author Haotian Zhang
 */
public class TsfSpringContextAwareTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ApplicationContextAwareUtils.class))
			.withPropertyValues("key1=value1");

	@Test
	public void testApplicationContextAwareUtils() {
		this.contextRunner.run(context -> {
			assertThat(TsfSpringContextAware.getApplicationContext()).isNotNull();
			TsfSpringContextAware tsfSpringContextAware = new TsfSpringContextAware();
			assertThatNoException().isThrownBy(() -> tsfSpringContextAware.setApplicationContext(context));

			// test getProperties
			assertThat(TsfSpringContextAware.getProperties("key1")).isEqualTo("value1");
			assertThat(TsfSpringContextAware.getProperties("key2")).isNull();

			// test getProperties with default value
			assertThat(TsfSpringContextAware.getProperties("key1", "defaultValue")).isEqualTo("value1");
			assertThat(TsfSpringContextAware.getProperties("key2", "defaultValue")).isEqualTo("defaultValue");

			// test getBean
			assertThat(TsfSpringContextAware.getBean(ApplicationContextAwareUtils.class)).isNotNull();
			assertThatThrownBy(() -> {
				TsfSpringContextAware.getBean(PolarisAsyncConfiguration.class);
			}).isInstanceOf(NoSuchBeanDefinitionException.class);
		});
	}
}
