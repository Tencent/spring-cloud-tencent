package com.tencent.cloud.polaris.config.configdata;

import com.tencent.polaris.api.utils.StringUtils;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PolarisImportExceptionFailureAnalyzer}.
 *
 * @author wlx
 * @date 2022/7/16 4:11 下午
 */
public class PolarisImportExceptionFailureAnalyzerTest {

	@Test
	public void failureAnalyzerTest() {
		SpringApplication app = mock(SpringApplication.class);
		MockEnvironment environment = new MockEnvironment();
		PolarisConfigDataMissingEnvironmentPostProcessor processor = new PolarisConfigDataMissingEnvironmentPostProcessor();
		assertThatThrownBy(() -> processor.postProcessEnvironment(environment, app))
				.isInstanceOf(PolarisConfigDataMissingEnvironmentPostProcessor.ImportException.class);
		Throwable throwable = catchThrowable(() -> processor.postProcessEnvironment(environment, app));
		PolarisImportExceptionFailureAnalyzer failureAnalyzer = new PolarisImportExceptionFailureAnalyzer();
		FailureAnalysis analyze = failureAnalyzer.analyze(throwable);
		assertThat(StringUtils.isNotBlank(analyze.getAction())).isTrue();
	}
}
