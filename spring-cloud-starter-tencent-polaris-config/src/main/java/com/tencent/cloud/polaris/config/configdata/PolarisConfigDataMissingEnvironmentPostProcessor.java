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
 */

package com.tencent.cloud.polaris.config.configdata;

import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.cloud.commons.ConfigDataMissingEnvironmentPostProcessor;
import org.springframework.cloud.util.PropertyUtils;
import org.springframework.core.env.Environment;

/**
 * PolarisConfigDataMissingEnvironmentPostProcessor to check if miss PolarisConfigData config,if miss config
 * will throw {@link ImportException}.
 *
 * @author wlx
 * @see ConfigDataMissingEnvironmentPostProcessor
 * @see ConfigDataMissingEnvironmentPostProcessor.ImportException
 */
public class PolarisConfigDataMissingEnvironmentPostProcessor extends ConfigDataMissingEnvironmentPostProcessor {

	/**
	 * run after {@link ConfigDataEnvironmentPostProcessor}.
	 */
	public static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER + 1;

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	protected boolean shouldProcessEnvironment(Environment environment) {
		// if using bootstrap or legacy processing don't run
		if (!PropertyUtils.bootstrapEnabled(environment) && !PropertyUtils.useLegacyProcessing(environment)) {
			boolean configEnabled = environment.getProperty("spring.cloud.polaris.config.enabled", Boolean.class, true);
			boolean importCheckEnabled = environment.getProperty("spring.cloud.polaris.config.import-check.enabled", Boolean.class, true);
			return configEnabled && importCheckEnabled;
		}
		else {
			return false;
		}
	}

	@Override
	protected String getPrefix() {
		return PolarisConfigDataLocationResolver.PREFIX;
	}
}
