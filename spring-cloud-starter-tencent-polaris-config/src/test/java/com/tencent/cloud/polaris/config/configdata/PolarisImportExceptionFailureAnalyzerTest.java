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

package com.tencent.cloud.polaris.config.configdata;

import com.tencent.polaris.api.utils.StringUtils;
import org.junit.jupiter.api.Test;

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
