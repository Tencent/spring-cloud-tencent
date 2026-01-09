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

package com.tencent.cloud.polaris.config.spring.annotation;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
				.withConfiguration(AutoConfigurations.of(TestBeanProperties3.class))
				.withConfiguration(AutoConfigurations.of(ComplexConfigurationProperties.class))
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
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.inner2.name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.set")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.list")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.list[0]")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.array")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.array[0]")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.map")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.map.key")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.timeUnit")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.time-unit")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.date")).isTrue();

			assertThat(springValueRegistry.isRefreshScopeKey("test.properties2.notExist")).isFalse();
			// @RefreshScope and @ConfigurationProperties on @Component bean
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties3.name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.properties3.notExist")).isFalse();
			// @RefreshScope and @Bean on method, @Value bean in class
			assertThat(springValueRegistry.isRefreshScopeKey("test.bean5.name")).isTrue();

			// Complex ConfigurationProperties tests
			// Primitive fields
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.port")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.enabled")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.timeout")).isTrue();
			// Enum and JDK built-in types
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.timeUnit")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.time-unit")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.createdDate")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.created-date")).isTrue();
			// Collections
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1List")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1-list")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1Map")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1-map")).isTrue();

			// Level 1 nested properties
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level1Name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level1-name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level1Value")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level1-value")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level1List")).isTrue();

			// Level 2 nested properties
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level2Name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level2-name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level2Enabled")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level2-enabled")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level2Map")).isTrue();

			// Level 3 nested properties
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level3Name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level3-name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level3Value")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level3-value")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level3Array")).isTrue();

			// Level 4 nested properties
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level4.level4Name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level4.level4-name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level4.level4Value")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level4.level4-value")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level4.level4Set")).isTrue();

			// Level 5 nested properties
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level4.level5.level5Name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level4.level5.level5-name")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level4.level5.level5Value")).isTrue();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level4.level5.level5-value")).isTrue();

			// Level 6 should not be registered due to depth limit (depth > 5)
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level4.level5.level6.level6Name")).isFalse();
			assertThat(springValueRegistry.isRefreshScopeKey("test.complex.properties.level1.level2.level3.level4.level5.level6.level6-name")).isFalse();

			// not self reference
			Field refreshScopeKeysField = SpringValueRegistry.class.getDeclaredField("refreshScopeKeys");
			refreshScopeKeysField.setAccessible(true);
			Set<String> callbackMap = (Set<String>) refreshScopeKeysField.get(springValueRegistry);
			long selfCont = callbackMap.stream().filter(key -> key.contains("self")).count();
			assertThat(selfCont).isEqualTo(0);
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

		private static String name;
		@Value("${timeout:1000}")
		private int timeout;
		ValueTest() {
		}

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

		private InnerProperties inner2;

		private TimeUnit timeUnit;

		private Date date;

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

		public InnerProperties getInner2() {
			return inner2;
		}

		public void setInner2(InnerProperties inner2) {
			this.inner2 = inner2;
		}

		public TimeUnit getTimeUnit() {
			return timeUnit;
		}

		public void setTimeUnit(TimeUnit timeUnit) {
			this.timeUnit = timeUnit;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
	}

	@Component
	@RefreshScope
	@ConfigurationProperties(prefix = "test.properties3")
	static class TestBeanProperties3 {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
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

	/**
	 * Complex ConfigurationProperties for testing various scenarios:
	 * - Self-referencing nested properties
	 * - Deep nesting (more than 5 levels)
	 * - Static fields
	 * - Final fields
	 * - Collections with complex types
	 * - Multiple levels of nested objects
	 */
	@Component
	@RefreshScope
	@ConfigurationProperties(prefix = "test.complex.properties")
	static class ComplexConfigurationProperties {

		// Static field - should be ignored by Spring
		private static String staticField = "static-value";

		// Final field with default value
		private final String finalField = "final-value";

		// Primitive types
		private String name;
		private int port;
		private boolean enabled;
		private long timeout;

		// Self-referencing nested property
		private ComplexConfigurationProperties self;

		// Level 1 nested object
		private Level1Properties level1;

		// Collection of complex types
		private ArrayList<Level1Properties> level1List;
		private HashMap<String, Level1Properties> level1Map;

		// Enum type
		private TimeUnit timeUnit;

		// JDK built-in types
		private Date createdDate;

		public static String getStaticField() {
			return staticField;
		}

		public static void setStaticField(String staticField) {
			ComplexConfigurationProperties.staticField = staticField;
		}

		public String getFinalField() {
			return finalField;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public long getTimeout() {
			return timeout;
		}

		public void setTimeout(long timeout) {
			this.timeout = timeout;
		}

		public ComplexConfigurationProperties getSelf() {
			return self;
		}

		public void setSelf(ComplexConfigurationProperties self) {
			this.self = self;
		}

		public Level1Properties getLevel1() {
			return level1;
		}

		public void setLevel1(Level1Properties level1) {
			this.level1 = level1;
		}

		public ArrayList<Level1Properties> getLevel1List() {
			return level1List;
		}

		public void setLevel1List(ArrayList<Level1Properties> level1List) {
			this.level1List = level1List;
		}

		public HashMap<String, Level1Properties> getLevel1Map() {
			return level1Map;
		}

		public void setLevel1Map(HashMap<String, Level1Properties> level1Map) {
			this.level1Map = level1Map;
		}

		public TimeUnit getTimeUnit() {
			return timeUnit;
		}

		public void setTimeUnit(TimeUnit timeUnit) {
			this.timeUnit = timeUnit;
		}

		public Date getCreatedDate() {
			return createdDate;
		}

		public void setCreatedDate(Date createdDate) {
			this.createdDate = createdDate;
		}
	}

	/**
	 * Level 1 nested properties
	 */
	static class Level1Properties {
		private String level1Name;
		private int level1Value;

		// Level 2 nested object
		private Level2Properties level2;

		// Collection at level 1
		private ArrayList<String> level1List;

		public String getLevel1Name() {
			return level1Name;
		}

		public void setLevel1Name(String level1Name) {
			this.level1Name = level1Name;
		}

		public int getLevel1Value() {
			return level1Value;
		}

		public void setLevel1Value(int level1Value) {
			this.level1Value = level1Value;
		}

		public Level2Properties getLevel2() {
			return level2;
		}

		public void setLevel2(Level2Properties level2) {
			this.level2 = level2;
		}

		public ArrayList<String> getLevel1List() {
			return level1List;
		}

		public void setLevel1List(ArrayList<String> level1List) {
			this.level1List = level1List;
		}
	}

	/**
	 * Level 2 nested properties
	 */
	static class Level2Properties {
		private String level2Name;
		private boolean level2Enabled;

		// Level 3 nested object
		private Level3Properties level3;

		// Map at level 2
		private HashMap<String, String> level2Map;

		public String getLevel2Name() {
			return level2Name;
		}

		public void setLevel2Name(String level2Name) {
			this.level2Name = level2Name;
		}

		public boolean isLevel2Enabled() {
			return level2Enabled;
		}

		public void setLevel2Enabled(boolean level2Enabled) {
			this.level2Enabled = level2Enabled;
		}

		public Level3Properties getLevel3() {
			return level3;
		}

		public void setLevel3(Level3Properties level3) {
			this.level3 = level3;
		}

		public HashMap<String, String> getLevel2Map() {
			return level2Map;
		}

		public void setLevel2Map(HashMap<String, String> level2Map) {
			this.level2Map = level2Map;
		}
	}

	/**
	 * Level 3 nested properties
	 */
	static class Level3Properties {
		private String level3Name;
		private double level3Value;

		// Level 4 nested object
		private Level4Properties level4;

		// Array at level 3
		private String[] level3Array;

		public String getLevel3Name() {
			return level3Name;
		}

		public void setLevel3Name(String level3Name) {
			this.level3Name = level3Name;
		}

		public double getLevel3Value() {
			return level3Value;
		}

		public void setLevel3Value(double level3Value) {
			this.level3Value = level3Value;
		}

		public Level4Properties getLevel4() {
			return level4;
		}

		public void setLevel4(Level4Properties level4) {
			this.level4 = level4;
		}

		public String[] getLevel3Array() {
			return level3Array;
		}

		public void setLevel3Array(String[] level3Array) {
			this.level3Array = level3Array;
		}
	}

	/**
	 * Level 4 nested properties
	 */
	static class Level4Properties {
		private String level4Name;
		private float level4Value;

		// Level 5 nested object
		private Level5Properties level5;

		// Set at level 4
		private HashSet<String> level4Set;

		public String getLevel4Name() {
			return level4Name;
		}

		public void setLevel4Name(String level4Name) {
			this.level4Name = level4Name;
		}

		public float getLevel4Value() {
			return level4Value;
		}

		public void setLevel4Value(float level4Value) {
			this.level4Value = level4Value;
		}

		public Level5Properties getLevel5() {
			return level5;
		}

		public void setLevel5(Level5Properties level5) {
			this.level5 = level5;
		}

		public HashSet<String> getLevel4Set() {
			return level4Set;
		}

		public void setLevel4Set(HashSet<String> level4Set) {
			this.level4Set = level4Set;
		}
	}

	/**
	 * Level 5 nested properties
	 */
	static class Level5Properties {
		// Final field at deep level
		private final String level5FinalField = "level5-final";
		private String level5Name;
		private byte level5Value;
		// Level 6 nested object - this will exceed the depth limit
		private Level6Properties level6;

		public String getLevel5Name() {
			return level5Name;
		}

		public void setLevel5Name(String level5Name) {
			this.level5Name = level5Name;
		}

		public byte getLevel5Value() {
			return level5Value;
		}

		public void setLevel5Value(byte level5Value) {
			this.level5Value = level5Value;
		}

		public Level6Properties getLevel6() {
			return level6;
		}

		public void setLevel6(Level6Properties level6) {
			this.level6 = level6;
		}

		public String getLevel5FinalField() {
			return level5FinalField;
		}
	}

	/**
	 * Level 6 nested properties - exceeds depth limit
	 */
	static class Level6Properties {
		private String level6Name;
		private short level6Value;

		public String getLevel6Name() {
			return level6Name;
		}

		public void setLevel6Name(String level6Name) {
			this.level6Name = level6Name;
		}

		public short getLevel6Value() {
			return level6Value;
		}

		public void setLevel6Value(short level6Value) {
			this.level6Value = level6Value;
		}
	}

}
