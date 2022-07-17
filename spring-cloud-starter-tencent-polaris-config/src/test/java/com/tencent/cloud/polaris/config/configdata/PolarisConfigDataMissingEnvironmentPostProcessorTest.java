package com.tencent.cloud.polaris.config.configdata;

import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PolarisConfigDataMissingEnvironmentPostProcessor}.
 *
 * @author wlx
 * @date 2022/7/16 4:09 下午
 */
public class PolarisConfigDataMissingEnvironmentPostProcessorTest {

	@Test
	public void missConfigData() {
		MockEnvironment environment = new MockEnvironment();
		SpringApplication app = mock(SpringApplication.class);
		PolarisConfigDataMissingEnvironmentPostProcessor processor = new PolarisConfigDataMissingEnvironmentPostProcessor();
		assertThatThrownBy(() -> processor.postProcessEnvironment(environment, app))
				.isInstanceOf(PolarisConfigDataMissingEnvironmentPostProcessor.ImportException.class);
	}

	@Test
	public void bootstrapEnabledTest() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.cloud.bootstrap.enabled", "true");
		SpringApplication app = mock(SpringApplication.class);
		PolarisConfigDataMissingEnvironmentPostProcessor processor = new PolarisConfigDataMissingEnvironmentPostProcessor();
		// if bootstrap enabled,don't throw ImportException
		assertThatCode(() -> processor.postProcessEnvironment(environment, app)).doesNotThrowAnyException();
	}

	@Test
	public void legacyProcessingTest() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.config.use-legacy-processing", "true");
		SpringApplication app = mock(SpringApplication.class);
		PolarisConfigDataMissingEnvironmentPostProcessor processor = new PolarisConfigDataMissingEnvironmentPostProcessor();
		// if use-legacy-processing,don't throw ImportException
		assertThatCode(() -> processor.postProcessEnvironment(environment, app)).doesNotThrowAnyException();
	}

	@Test
	public void closeImportCheck() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.cloud.polaris.config.import-check.enabled", "false");
		SpringApplication app = mock(SpringApplication.class);
		PolarisConfigDataMissingEnvironmentPostProcessor processor = new PolarisConfigDataMissingEnvironmentPostProcessor();
		// if import-check.enabled is false,don't throw ImportException
		assertThatCode(() -> processor.postProcessEnvironment(environment, app)).doesNotThrowAnyException();
	}

	@Test
	public void closePolarisConfig() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.cloud.polaris.config.enabled", "false");
		SpringApplication app = mock(SpringApplication.class);
		PolarisConfigDataMissingEnvironmentPostProcessor processor = new PolarisConfigDataMissingEnvironmentPostProcessor();
		// if polaris.config is false,don't throw ImportException
		assertThatCode(() -> processor.postProcessEnvironment(environment, app)).doesNotThrowAnyException();
	}

	@Test
	public void normalConfigDataImport() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.config.import", "polaris");
		SpringApplication app = mock(SpringApplication.class);
		PolarisConfigDataMissingEnvironmentPostProcessor processor = new PolarisConfigDataMissingEnvironmentPostProcessor();
		// config polaris config import ,don't throw ImportException
		assertThatCode(() -> processor.postProcessEnvironment(environment, app)).doesNotThrowAnyException();
	}

	@Test
	public void importOtherConfigDataWithoutPolaris() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.config.import", "file:application.properties");
		SpringApplication app = mock(SpringApplication.class);
		PolarisConfigDataMissingEnvironmentPostProcessor processor = new PolarisConfigDataMissingEnvironmentPostProcessor();
		assertThatThrownBy(() -> processor.postProcessEnvironment(environment, app))
				.isInstanceOf(PolarisConfigDataMissingEnvironmentPostProcessor.ImportException.class);
	}
}
