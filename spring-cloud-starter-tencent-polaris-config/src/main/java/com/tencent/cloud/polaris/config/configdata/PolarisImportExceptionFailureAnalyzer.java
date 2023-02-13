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

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.cloud.commons.ConfigDataMissingEnvironmentPostProcessor;

/**
 * Class for most {@code FailureAnalyzer} implementations, to analyze ImportException when
 * miss Polaris configData config.
 * <p>Refer to the Nacos project implementation:
 * <code><a href=https://github.com/alibaba/spring-cloud-alibaba/blob/2021.x/spring-cloud-alibaba-starters/spring-cloud-starter-alibaba-nacos-config/src/main/java/com/alibaba/cloud/nacos/configdata/NacosConfigDataMissingEnvironmentPostProcessor.java>
 * ImportExceptionFailureAnalyzer</a></code>
 *
 * @author wlx
 * @see AbstractFailureAnalyzer
 */
public class PolarisImportExceptionFailureAnalyzer extends
		AbstractFailureAnalyzer<ConfigDataMissingEnvironmentPostProcessor.ImportException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ConfigDataMissingEnvironmentPostProcessor.ImportException cause) {
		String description;
		if (cause.missingPrefix) {
			description = "The spring.config.import property is missing a " + PolarisConfigDataLocationResolver.PREFIX
					+ " entry";
		}
		else {
			description = "No spring.config.import property has been defined";
		}
		String action = "\t1. Add a spring.config.import=polaris property to your configuration.\n"
				+ "\t2. If configuration is not required add spring.config.import=optional:polaris instead.\n"
				+ "\t3. If you still want use bootstrap.yml, "
				+ "you can add <groupId>org.springframework.cloud</groupId> <artifactId>spring-cloud-starter-bootstrap</artifactId> dependency for compatible upgrade.";
		return new FailureAnalysis(description, action, cause);
	}
}
