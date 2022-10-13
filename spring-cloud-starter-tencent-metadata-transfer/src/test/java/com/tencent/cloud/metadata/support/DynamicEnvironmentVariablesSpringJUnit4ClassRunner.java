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

package com.tencent.cloud.metadata.support;

import java.util.Arrays;
import java.util.Objects;

import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runners.model.InitializationError;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Enhance {@link SpringJUnit4ClassRunner} to support add environment variables when Junit4 test run.
 *
 *
 * @author lingxiao.wlx
 * @see DynamicEnvironmentVariable
 * @see DynamicEnvironmentVariables
 */
public class DynamicEnvironmentVariablesSpringJUnit4ClassRunner extends SpringJUnit4ClassRunner {

	/**
	 * EnvironmentVariables.
	 */
	@Rule
	public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

	public DynamicEnvironmentVariablesSpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		DynamicEnvironmentVariable dynamicEnvironmentVariable = AnnotationUtils.findAnnotation(clazz, DynamicEnvironmentVariable.class);
		if (!Objects.isNull(dynamicEnvironmentVariable)) {
			String key = dynamicEnvironmentVariable.name();
			String value = dynamicEnvironmentVariable.value();
			environmentVariables.set(key, value);
		}
		DynamicEnvironmentVariables dynamicEnvironmentVariables = AnnotationUtils.findAnnotation(clazz, DynamicEnvironmentVariables.class);
		if (!Objects.isNull(dynamicEnvironmentVariables)) {
			Arrays.stream(dynamicEnvironmentVariables.value()).forEach(
					environmentVariable -> {
						environmentVariables.set(environmentVariable.name(), environmentVariable.value());
					}
			);
		}
	}
}
