/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.config.spring.annotation;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import com.tencent.cloud.polaris.config.PolarisConfigBootstrapAutoConfiguration;
import com.tencent.cloud.polaris.config.enums.RefreshType;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SpringValueProcessor}.
 *
 * @author Shedfree Wu
 */
public class RefreshScopeSpringProcessorTest {

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
				.withConfiguration(AutoConfigurations.of(TestConfig2.class))
				.withConfiguration(AutoConfigurations.of(TestConfig3.class))
				.withConfiguration(AutoConfigurations.of(TestConfig4.class))
				.withConfiguration(AutoConfigurations.of(TestConfig5.class))
				.withConfiguration(AutoConfigurations.of(TestBeanProperties1.class))
				.withConfiguration(AutoConfigurations.of(TestBeanProperties2.class))
				.withConfiguration(AutoConfigurations.of(PolarisConfigAutoConfiguration.class))
				.withAllowBeanDefinitionOverriding(true)
				.withPropertyValues("spring.application.name=" + "conditionalOnConfigReflectEnabledTest")
				.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
				.withPropertyValues("spring.cloud.polaris.config.refresh-type=" + RefreshType.REFLECT)
				.withPropertyValues("spring.cloud.polaris.config.enabled=true")
				.withPropertyValues("timeout=10000");
		contextRunner.run(context -> {
			SpringValueRegistry springValueRegistry = context.getBean(SpringValueRegistry.class);

			assertThat(springValueRegistry.isRefreshScopeKey("key.not.exist")).isFalse();
			// @RefreshScope on @Component bean, @Value on field
			assertThat(springValueRegistry.isRefreshScopeKey("timeout")).isTrue();
			// not exact match
			assertThat(springValueRegistry.isRefreshScopeKey("timeout.test")).isFalse();
			// @RefreshScope on @Component bean, @Value on method
			assertThat(springValueRegistry.isRefreshScopeKey("name")).isTrue();
			// @RefreshScope and @Bean on method, @Value on field
			assertThat(springValueRegistry.isRefreshScopeKey("test.bean.name")).isTrue();
			// @RefreshScope and @Bean on method, @Value on method
			assertThat(springValueRegistry.isRefreshScopeKey("test.bean.timeout")).isTrue();
			// @RefreshScope and @Bean on method, @Value on parameter
			assertThat(springValueRegistry.isRefreshScopeKey("test.param.name")).isTrue();
			// @RefreshScope and @Bean on method, @ConfigurationProperties bean on method parameter
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties1.name")).isTrue();
			// @RefreshScope and @Bean on method, @ConfigurationProperties bean in class
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.inner.name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.set")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.list")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.list[0]")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.array")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.array[0]")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.map")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.map.key")).isTrue();

			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.notExist")).isFalse();
			// @RefreshScope and @Bean on method, @Value bean in class
			assertThat(springValueRegistry.isRefreshScopeKey("test.bean5.name")).isTrue();
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
	@RefreshScope
	private static class ValueTest {

		ValueTest() {
		}

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
	static class TestConfig2 {
		@Bean
		@RefreshScope
		public TestBean testBean2() {
			return new TestBean();
		}
	}

	@Configuration
	static class TestConfig3 {
		@Bean
		@RefreshScope
		public TestBean testBean3(@Value("${test.param.name:}") String name) {
			return new TestBean();
		}
	}

	@Configuration
	static class TestConfig4 {
		@Bean
		@RefreshScope
		public TestBean testBean4(TestBeanProperties1 testBeanProperties1) {
			return new TestBean();
		}
	}

	@Configuration
	static class TestConfig5 {

		@Autowired
		private TestBeanProperties2 testBeanProperties2;

		@Value("${test.bean5.name:}")
		private String name;

		@Bean
		@RefreshScope
		public TestBean testBean5() {
			TestBean testBean = new TestBean();
			testBean.setName(testBeanProperties2.getName());
			return testBean;
		}
	}

	static class TestBean {

		@Value("${test.bean.name:}")
		private String name;

		private int timeout;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getTimeout() {
			return timeout;
		}

		@Value("${test.bean.timeout:0}")
		public void setTimeout(int timeout) {
			this.timeout = timeout;
		}
	}

	@Component
	@ConfigurationProperties(prefix = "test.properties1")
	static class TestBeanProperties1 {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Component
	@ConfigurationProperties("test.properties2")
	static class TestBeanProperties2 {
		private String name;

		private HashSet<String> set;

		private ArrayList<String> list;

		private String[] array;

		private HashMap<String, String> map;

		private InnerProperties inner;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public HashSet<String> getSet() {
			return set;
		}

		public void setSet(HashSet<String> set) {
			this.set = set;
		}

		public ArrayList<String> getList() {
			return list;
		}

		public void setList(ArrayList<String> list) {
			this.list = list;
		}

		public String[] getArray() {
			return array;
		}

		public void setArray(String[] array) {
			this.array = array;
		}

		public HashMap<String, String> getMap() {
			return map;
		}

		public void setMap(HashMap<String, String> map) {
			this.map = map;
		}

		public InnerProperties getInner() {
			return inner;
		}

		public void setInner(InnerProperties inner) {
			this.inner = inner;
		}
	}

	static class InnerProperties {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
