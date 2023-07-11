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

package com.tencent.cloud.polaris.config.adapter;

import com.tencent.cloud.polaris.config.PolarisConfigAutoConfiguration;
import com.tencent.cloud.polaris.config.PolarisConfigBootstrapAutoConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * test for {@link PolarisConfigRefreshScopeAnnotationDetector}.
 */
@SuppressWarnings("rawtypes")
public class PolarisConfigRefreshScopeAnnotationDetectorTest {

	private static Class refreshScopeAnnotationClass = null;

	static {
		try {
			refreshScopeAnnotationClass = Class.forName(
					"org.springframework.cloud.context.config.annotation.RefreshScope",
					false,
					PolarisConfigRefreshScopeAnnotationDetectorTest.class.getClassLoader());
		}
		catch (ClassNotFoundException ignored) {
		}
	}

	@Test
	public void testUseRefreshScope() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisConfigBootstrapAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(PolarisConfigAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(ConfigurationPropertiesRebinderAutoConfiguration.class))
				.withBean("testBeanWithRefreshScope", TestBeanWithRefreshScope.class)
				.withPropertyValues("spring.application.name=" + "polarisConfigRefreshScopeAnnotationDetectorTest")
				.withPropertyValues("server.port=" + 8080)
				.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
				.withPropertyValues("spring.cloud.polaris.config.connect-remote-server=false");
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(PolarisConfigRefreshScopeAnnotationDetector.class);
			PolarisConfigRefreshScopeAnnotationDetector detector = context.getBean(PolarisConfigRefreshScopeAnnotationDetector.class);
			assertThat(detector.isRefreshScopeAnnotationUsed()).isTrue();
			assertThat(detector.getAnnotatedRefreshScopeBeanName()).isEqualTo("scopedTarget.testBeanWithRefreshScope");
			assertThat(detector).extracting("refreshScopeAnnotationClass", as(InstanceOfAssertFactories.type(Class.class)))
					.isEqualTo(refreshScopeAnnotationClass);
		});
	}

	@Test
	public void testNotUseRefreshScope() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisConfigBootstrapAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(PolarisConfigAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(ConfigurationPropertiesRebinderAutoConfiguration.class))
				.withPropertyValues("spring.application.name=" + "polarisConfigRefreshScopeAnnotationDetectorTest")
				.withPropertyValues("server.port=" + 8080)
				.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
				.withPropertyValues("spring.cloud.polaris.config.connect-remote-server=false");
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(PolarisConfigRefreshScopeAnnotationDetector.class);
			PolarisConfigRefreshScopeAnnotationDetector detector = context.getBean(PolarisConfigRefreshScopeAnnotationDetector.class);
			assertThat(detector.isRefreshScopeAnnotationUsed()).isFalse();
			assertThat(detector.getAnnotatedRefreshScopeBeanName()).isNull();
			assertThat(detector).extracting("refreshScopeAnnotationClass", as(InstanceOfAssertFactories.type(Class.class)))
					.isEqualTo(refreshScopeAnnotationClass);
		});
	}

	@RefreshScope
	protected static class TestBeanWithRefreshScope {

	}
}
