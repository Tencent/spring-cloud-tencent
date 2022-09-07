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

package com.tencent.cloud.polaris.config.spring.annotation;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import com.tencent.cloud.polaris.config.PolarisConfigBootstrapAutoConfiguration;
import com.tencent.cloud.polaris.config.enums.RefreshType;
import com.tencent.cloud.polaris.config.spring.property.SpringValue;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import com.tencent.polaris.api.utils.CollectionUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Test for {@link SpringValueProcessor}.
 *
 * @author lingxiao.wlx
 */
public class SpringValueProcessorTest {

	private static ServerSocket serverSocket;

	@BeforeClass
	public static void before() {
		new Thread(() -> {
			try {
				serverSocket = new ServerSocket(8093);
				serverSocket.accept();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	@AfterClass
	public static void after() throws IOException {
		serverSocket.close();
	}

	@Test
	public void springValueFiledProcessorTest() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisConfigBootstrapAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(ValueTest.class))
				.withConfiguration(AutoConfigurations.of(PolarisConfigAutoConfiguration.class))
				.withPropertyValues("spring.application.name=" + "conditionalOnConfigReflectEnabledTest")
				.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
				.withPropertyValues("spring.cloud.polaris.config.refresh-type=" + RefreshType.REFLECT)
				.withPropertyValues("spring.cloud.polaris.config.enabled=true")
				.withPropertyValues("timeout=10000");
		contextRunner.run(context -> {
			SpringValueRegistry springValueRegistry = context.getBean(SpringValueRegistry.class);
			PolarisConfigAutoConfiguration polarisConfigAutoConfiguration = context.getBean(PolarisConfigAutoConfiguration.class);
			BeanFactory beanFactory = polarisConfigAutoConfiguration.beanFactory;
			Collection<SpringValue> timeout = springValueRegistry.get(beanFactory, "timeout");
			Assert.assertFalse(CollectionUtils.isEmpty(timeout));
			Optional<SpringValue> springValueOptional = timeout.stream().findAny();
			Assert.assertTrue(springValueOptional.isPresent());

			SpringValue springValue = springValueOptional.get();
			Assert.assertEquals("${timeout:1000}", springValue.getPlaceholder());
			Assert.assertTrue(springValue.isField());
			Assert.assertTrue(Objects.nonNull(springValue.getField()));
			Assert.assertEquals("timeout", springValue.getField().getName());
			Assert.assertEquals(int.class, springValue.getTargetType());

			ValueTest bean = context.getBean(ValueTest.class);
			Assert.assertEquals(10000, bean.timeout);
		});
	}

	@Test
	public void springValueMethodProcessorTest() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisConfigBootstrapAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(ValueTest.class))
				.withConfiguration(AutoConfigurations.of(PolarisConfigAutoConfiguration.class))
				.withPropertyValues("spring.application.name=" + "conditionalOnConfigReflectEnabledTest")
				.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
				.withPropertyValues("spring.cloud.polaris.config.refresh-type=" + RefreshType.REFLECT)
				.withPropertyValues("spring.cloud.polaris.config.enabled=true")
				.withPropertyValues("name=test");
		contextRunner.run(context -> {
			SpringValueRegistry springValueRegistry = context.getBean(SpringValueRegistry.class);
			PolarisConfigAutoConfiguration polarisConfigAutoConfiguration = context.getBean(PolarisConfigAutoConfiguration.class);
			BeanFactory beanFactory = polarisConfigAutoConfiguration.beanFactory;
			Collection<SpringValue> name = springValueRegistry.get(beanFactory, "name");
			Assert.assertFalse(CollectionUtils.isEmpty(name));
			Optional<SpringValue> springValueOptional = name.stream().findAny();
			Assert.assertTrue(springValueOptional.isPresent());

			SpringValue springValue = springValueOptional.get();
			Method method = springValue.getMethodParameter().getMethod();
			Assert.assertTrue(Objects.nonNull(method));
			Assert.assertEquals("setName", method.getName());
			Assert.assertEquals("${name:1000}", springValue.getPlaceholder());
			Assert.assertFalse(springValue.isField());
			Assert.assertEquals(String.class, springValue.getTargetType());

			Assert.assertEquals("test", ValueTest.name);
		});
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisConfigAutoConfiguration {

		@Autowired
		private BeanFactory beanFactory;

		public BeanFactory getBeanFactory() {
			return beanFactory;
		}

		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}
	}

	@Component
	private static class ValueTest {
		private static String name;
		@Value("${timeout:1000}")
		private int timeout;

		public int getTimeout() {
			return timeout;
		}

		public void setTimeout(int timeout) {
			this.timeout = timeout;
		}

		@Value("${name:1000}")
		public void setName(String name) {
			ValueTest.name = name;
		}
	}
}
