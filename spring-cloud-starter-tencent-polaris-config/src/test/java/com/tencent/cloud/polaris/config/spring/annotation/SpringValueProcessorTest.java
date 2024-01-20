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
import com.tencent.cloud.polaris.config.spring.property.Person;
import com.tencent.cloud.polaris.config.spring.property.SpringValue;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import com.tencent.polaris.api.utils.CollectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SpringValueProcessor}.
 *
 * @author lingxiao.wlx
 */
public class SpringValueProcessorTest {

	private static ServerSocket serverSocket;

	@BeforeAll
	static void beforeAll() {
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

	@AfterAll
	static void afterAll() throws IOException {
		if (Objects.nonNull(serverSocket)) {
			serverSocket.close();
		}
	}

	@Test
	public void springValueFiledProcessorTest() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisConfigBootstrapAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(ValueTest.class))
				.withConfiguration(AutoConfigurations.of(PolarisConfigAutoConfiguration.class))
				.withAllowBeanDefinitionOverriding(true)
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
			assertThat(CollectionUtils.isEmpty(timeout)).isFalse();
			Optional<SpringValue> springValueOptional = timeout.stream().findAny();
			assertThat(springValueOptional.isPresent()).isTrue();

			SpringValue springValue = springValueOptional.get();
			assertThat(springValue.getPlaceholder()).isEqualTo("${timeout:1000}");
			assertThat(springValue.isField()).isTrue();
			assertThat(springValue.getField()).isNotNull();
			assertThat(springValue.getField().getName()).isEqualTo("timeout");
			assertThat(springValue.getTargetType()).isEqualTo(int.class);

			ValueTest bean = context.getBean(ValueTest.class);
			assertThat(bean.timeout).isEqualTo(10000);
		});
	}

	@Test
	public void springValueMethodProcessorTest() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisConfigBootstrapAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(ValueTest.class))
				.withConfiguration(AutoConfigurations.of(PolarisConfigAutoConfiguration.class))
				.withAllowBeanDefinitionOverriding(true)
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
			assertThat(name).isNotEmpty();
			Optional<SpringValue> springValueOptional = name.stream().findAny();
			assertThat(springValueOptional.isPresent()).isTrue();

			SpringValue springValue = springValueOptional.get();
			Method method = springValue.getMethodParameter().getMethod();
			assertThat(method).isNotNull();
			assertThat(method.getName()).isEqualTo("setName");
			assertThat(springValue.getPlaceholder()).isEqualTo("${name:1000}");
			assertThat(springValue.isField()).isFalse();
			assertThat(springValue.getTargetType()).isEqualTo(String.class);

			assertThat(ValueTest.name).isEqualTo("test");
		});
	}

	@Test
	public void xmlBeamDefinitionTest() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisConfigBootstrapAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(XMLBeamDefinitionTest.class))
				.withConfiguration(AutoConfigurations.of(PolarisConfigAutoConfiguration.class))
				.withAllowBeanDefinitionOverriding(true)
				.withPropertyValues("spring.application.name=" + "conditionalOnConfigReflectEnabledTest")
				.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
				.withPropertyValues("spring.cloud.polaris.config.refresh-type=" + RefreshType.REFLECT)
				.withPropertyValues("spring.cloud.polaris.config.enabled=true")
				.withPropertyValues("name=test");
		contextRunner.run(context -> {
			Person person = context.getBean(Person.class);

			SpringValueRegistry springValueRegistry = context.getBean(SpringValueRegistry.class);
			BeanFactory beanFactory = person.getBeanFactory();
			Collection<SpringValue> name = springValueRegistry.get(beanFactory, "name");
			assertThat(name).isNotEmpty();
			Optional<SpringValue> nameSpringValueOptional = name.stream().findAny();
			assertThat(nameSpringValueOptional.isPresent()).isTrue();

			SpringValue nameSpringValue = nameSpringValueOptional.get();
			Method method = nameSpringValue.getMethodParameter().getMethod();
			assertThat(method).isNotNull();
			assertThat(method.getName()).isEqualTo("setName");
			assertThat(nameSpringValue.getPlaceholder()).isEqualTo("${name:test}");
			assertThat(nameSpringValue.isField()).isFalse();
			assertThat(nameSpringValue.getTargetType()).isEqualTo(String.class);


			Collection<SpringValue> age = springValueRegistry.get(beanFactory, "age");
			assertThat(age).isNotEmpty();
			Optional<SpringValue> ageSpringValueOptional = age.stream().findAny();
			assertThat(ageSpringValueOptional.isPresent()).isTrue();

			SpringValue ageSpringValue = ageSpringValueOptional.get();
			Method method1 = ageSpringValue.getMethodParameter().getMethod();
			assertThat(method1).isNotNull();
			assertThat(method1.getName()).isEqualTo("setAge");
			assertThat(ageSpringValue.getPlaceholder()).isEqualTo("${age:10}");
			assertThat(ageSpringValue.isField()).isFalse();
			assertThat(ageSpringValue.getTargetType()).isEqualTo(String.class);
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

	@Configuration
	@ImportResource("classpath:bean.xml")
	static class XMLBeamDefinitionTest {
	}
}
