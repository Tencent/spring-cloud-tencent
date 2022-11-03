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

package com.tencent.cloud.common.support;

import java.util.Arrays;
import java.util.Objects;

import org.junit.Rule;
import org.junit.runners.model.InitializationError;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Enhance {@link SpringJUnit4ClassRunner} to support inject environment variable during test running.
 * <p>
 * typical usage idiom for this class would be:
 * <pre> {@code
 * @RunWith(EnvironmentVariableInjectedRunner.class)
 * @EnvironmentVariable(name = "name",value = "value")
 * public class Test {
 * }
 * }</pre>
 *
 * @author lingxiao.wlx
 * @see EnvironmentVariable
 * @see EnvironmentVariables
 */
public class EnvironmentVariableInjectedRunner extends SpringJUnit4ClassRunner {

	@Rule
	private final org.junit.contrib.java.lang.system.EnvironmentVariables injectedEnvironmentVariables
			= new org.junit.contrib.java.lang.system.EnvironmentVariables();

	public EnvironmentVariableInjectedRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		EnvironmentVariable environmentVariable = AnnotationUtils.findAnnotation(clazz, EnvironmentVariable.class);
		if (!Objects.isNull(environmentVariable)) {
			injectEnvironmentVariable(environmentVariable);
		}
		EnvironmentVariables environmentVariables = AnnotationUtils.findAnnotation(clazz, EnvironmentVariables.class);
		if (!Objects.isNull(environmentVariables)) {
			injectEnvironmentVariables(environmentVariables);
		}
	}

	private void injectEnvironmentVariables(EnvironmentVariables environmentVariables) {
		Arrays.stream(environmentVariables.value()).forEach(
				this::injectEnvironmentVariable
		);
	}

	private void injectEnvironmentVariable(EnvironmentVariable environmentVariable) {
		injectedEnvironmentVariables.set(environmentVariable.name(), environmentVariable.value());
	}
}
