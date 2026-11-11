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

package com.tencent.cloud.polaris.contract.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfCoreEnvironmentPostProcessor;
import com.tencent.cloud.polaris.contract.SwaggerContext;
import com.tencent.cloud.polaris.contract.filter.ApiDocServletFilter;
import com.tencent.cloud.polaris.contract.tsf.TsfApiMetadataGrapher;
import com.tencent.cloud.polaris.contract.tsf.TsfSwaggerAutoConfiguration;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springdoc.core.models.GroupedOpenApi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * Test legacy Swagger configuration binding and consumption.
 *
 * @author fuyuwei01
 */
@DisplayName("TSF Swagger properties")
class TsfSwaggerPropertiesTest {

	private static final String BASE_PACKAGE = "com.tencent.cloud.polaris.contract";

	private TsfCoreEnvironmentPostProcessor processor;

	@BeforeEach
	void setUp() throws Exception {
		Constructor<TsfCoreEnvironmentPostProcessor> constructor =
				TsfCoreEnvironmentPostProcessor.class.getDeclaredConstructor(DeferredLogFactory.class);
		constructor.setAccessible(true);
		DeferredLogFactory logFactory = destination -> LogFactory.getLog(TsfSwaggerPropertiesTest.class);
		processor = constructor.newInstance(logFactory);
		SwaggerContext.setAttribute("$MainClass", TestApplication.class);
	}

	/**
	 * Verify late remote strings reach both the API group and TSF metadata grapher.
	 */
	@DisplayName("Remote Swagger strings configure the API group and TSF grapher")
	@Test
	void testRemoteSwaggerStrings() throws Exception {
		// Arrange
		MockEnvironment environment = createEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("remote-config", Map.of(
				"tsf.swagger.group", "remote-group",
				"tsf.swagger.basePackage", BASE_PACKAGE,
				"tsf.swagger.excludePath", "/health,/info",
				"tsf.swagger.basePath", "/api/**,/admin/**")));
		Field groupName = TsfApiMetadataGrapher.class.getDeclaredField("groupName");
		groupName.setAccessible(true);

		// Act
		contextRunner(environment).run(context -> {
			PolarisContractProperties properties = context.getBean(PolarisContractProperties.class);
			GroupedOpenApi api = new PolarisSwaggerAutoConfiguration().polarisGroupedOpenApi(properties);
			TsfApiMetadataGrapher grapher = new TsfSwaggerAutoConfiguration().tsfApiMetadataGrapher(
					null, null, context, properties, null, environment);

			// Assert
			assertThat(api.getGroup()).isEqualTo("remote-group");
			assertThat(api.getPackagesToScan()).containsExactly(BASE_PACKAGE);
			assertThat(api.getPathsToExclude()).containsExactly("/health", "/info");
			assertThat(api.getPathsToMatch()).containsExactly("/api/**", "/admin/**");
			assertThat(groupName.get(grapher)).isEqualTo(api.getGroup());
		});
	}

	/**
	 * Verify a remote legacy false disables the actual Swagger auto-configuration.
	 */
	@DisplayName("Remote Swagger disabled prevents API group creation and reporting")
	@Test
	void testRemoteSwaggerDisabled() throws Exception {
		// Arrange
		MockEnvironment environment = createEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("remote-config",
				Map.of("tsf.swagger.enabled", "false")));

		// Act
		contextRunner(environment).withUserConfiguration(PolarisSwaggerAutoConfiguration.class).run(context -> {
			PolarisContractProperties properties = context.getBean(PolarisContractProperties.class);

			// Assert
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(GroupedOpenApi.class);
			assertThat(context).doesNotHaveBean(PolarisSwaggerAutoConfiguration.class);
			assertThat(properties.isEnabled()).isFalse();
			assertThat(properties.isReportEnabled()).isFalse();
		});
	}

	/**
	 * Verify the document flag binds independently of contract and report enablement.
	 */
	@DisplayName("Remote document flag disables exposure while keeping reporting enabled")
	@Test
	void testRemoteSwaggerExposureDisabled() throws Exception {
		// Arrange
		MockEnvironment environment = createEnvironment();
		environment.getPropertySources().addAfter("tsf-polaris-properties", new MapPropertySource("remote-config",
				Map.of("tsf.swagger.doc.auto-startup", "false")));

		// Act
		contextRunner(environment).run(context -> {
			PolarisContractProperties properties = context.getBean(PolarisContractProperties.class);
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setServletPath("/v3/api-docs");
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();
			new ApiDocServletFilter(properties).doFilter(request, response, chain);

			// Assert
			assertThat(properties.isExposure()).isFalse();
			assertThat(properties.isEnabled()).isTrue();
			assertThat(properties.isReportEnabled()).isTrue();
			assertThat(response.getStatus()).isEqualTo(403);
			assertThat(chain.getRequest()).isNull();
		});
	}

	/**
	 * Verify local legacy keys loaded after the processor configure the API group.
	 */
	@DisplayName("Local configuration loaded after bootstrap configures the Swagger group")
	@Test
	void testLocalSwaggerConfiguration() throws Exception {
		// Arrange
		MockEnvironment environment = createEnvironment();
		environment.getPropertySources().addAfter("tsf-polaris-properties", new MapPropertySource("application.yml",
				Map.of("tsf.swagger.group", "local-group", "tsf.swagger.basePath", "/local/**")));

		// Act
		contextRunner(environment).run(context -> {
			GroupedOpenApi api = new PolarisSwaggerAutoConfiguration()
					.polarisGroupedOpenApi(context.getBean(PolarisContractProperties.class));

			// Assert
			assertThat(api.getGroup()).isEqualTo("local-group");
			assertThat(api.getPathsToMatch()).containsExactly("/local/**");
		});
	}

	/**
	 * Verify a real YAML file loaded by Config Data supplies legacy Swagger properties.
	 */
	@DisplayName("Config Data loads legacy Swagger configuration from a YAML file")
	@Test
	void testSwaggerConfigDataFile(@TempDir Path directory) throws Exception {
		// Arrange
		MockEnvironment environment = createEnvironment();
		Path config = directory.resolve("application.yml");
		Files.writeString(config, "tsf:\n"
				+ "  swagger:\n"
				+ "    group: file-group\n"
				+ "    enabled: false\n"
				+ "    basePackage: com.example.file\n"
				+ "    excludePath: /health\n"
				+ "    basePath: /file/**\n"
				+ "    doc:\n"
				+ "      auto-startup: false\n");
		environment.setProperty("spring.config.location", config.toUri().toString());

		// Act
		ConfigDataEnvironmentPostProcessor.applyTo(environment);
		contextRunner(environment).run(context -> {
			PolarisContractProperties properties = context.getBean(PolarisContractProperties.class);

			// Assert
			assertThat(properties.getGroup()).isEqualTo("file-group");
			assertThat(properties.getBasePackage()).isEqualTo("com.example.file");
			assertThat(properties.getExcludePath()).isEqualTo("/health");
			assertThat(properties.getBasePath()).isEqualTo("/file/**");
			assertThat(properties.isEnabled()).isFalse();
			assertThat(properties.isReportEnabled()).isFalse();
			assertThat(properties.isExposure()).isFalse();
		});
	}

	/**
	 * Verify absent or empty optional strings keep application scanning and no exclusions.
	 */
	@DisplayName("Empty optional Swagger strings preserve application scanning defaults")
	@Test
	void testEmptySwaggerScanConfiguration() throws Exception {
		// Arrange
		MockEnvironment environment = createEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("remote-config", Map.of(
				"tsf.swagger.basePackage", "", "tsf.swagger.excludePath", "")));

		// Act
		contextRunner(environment).run(context -> {
			PolarisContractProperties properties = context.getBean(PolarisContractProperties.class);
			GroupedOpenApi api = new PolarisSwaggerAutoConfiguration().polarisGroupedOpenApi(properties);

			// Assert
			assertThat(api.getGroup()).isEqualTo("polaris");
			assertThat(api.getPackagesToScan()).containsExactly(BASE_PACKAGE);
			assertThat(api.getPathsToExclude()).isEmpty();
			assertThat(api.getPathsToMatch()).containsExactly("/**");
			assertThat(properties.isExposure()).isTrue();
		});
	}

	private ApplicationContextRunner contextRunner(MockEnvironment environment) {
		return new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
				.withUserConfiguration(PolarisContractPropertiesAutoConfiguration.class)
				.withInitializer(context -> context.setEnvironment(environment));
	}

	private MockEnvironment createEnvironment() throws Exception {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("tsf_app_id", "app-test");
		environment.setProperty("tsf_consul_ip", "127.0.0.1");
		environment.setProperty("tsf_consul_port", "8500");
		environment.setProperty("tsf_token", "test-token");
		environment.setProperty("tsf_instance_id", "instance-test");
		environment.setProperty("tsf_application_id", "application-test");
		environment.setProperty("tsf_group_id", "group-test");
		environment.setProperty("tsf_namespace_id", "namespace-test");
		try (MockedStatic<TsfContextUtils> tsfContextUtils = mockStatic(TsfContextUtils.class)) {
			tsfContextUtils.when(() -> TsfContextUtils.isTsfConsulEnabled(environment)).thenReturn(true);
			tsfContextUtils.when(() -> TsfContextUtils.isOnlyTsfConsulEnabled(environment)).thenReturn(true);
			processor.postProcessEnvironment(environment, new SpringApplication());
		}
		return environment;
	}

	@SpringBootApplication(scanBasePackages = BASE_PACKAGE)
	private static class TestApplication {
	}
}
