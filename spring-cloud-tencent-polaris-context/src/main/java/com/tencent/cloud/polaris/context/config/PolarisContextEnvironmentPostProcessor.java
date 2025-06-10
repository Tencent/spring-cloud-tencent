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

package com.tencent.cloud.polaris.context.config;

import java.util.HashMap;
import java.util.Map;

import com.tencent.polaris.api.utils.StringUtils;
import org.apache.commons.logging.Log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Read Polaris env.
 *
 * @author Haotian Zhang
 */
public final class PolarisContextEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * run before {@link ConfigDataEnvironmentPostProcessor}.
	 */
	public static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER - 2;

	private final Log LOGGER;

	private PolarisContextEnvironmentPostProcessor(DeferredLogFactory logFactory) {
		this.LOGGER = logFactory.getLog(getClass());
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> polarisEnvProperties = new HashMap<>();

		// polaris_address
		String polarisAddress = environment.getProperty("polaris_address");
		if (StringUtils.isNotBlank(polarisAddress)) {
			polarisEnvProperties.put("spring.cloud.polaris.address", polarisAddress);
		}

		// polaris_address
		String polarisNamespace = environment.getProperty("polaris_namespace");
		if (StringUtils.isNotBlank(polarisNamespace)) {
			polarisEnvProperties.put("spring.cloud.polaris.namespace", polarisNamespace);
		}

		// polaris_config_address
		String polarisConfigAddress = environment.getProperty("polaris_config_address");
		if (StringUtils.isNotBlank(polarisConfigAddress)) {
			polarisEnvProperties.put("spring.cloud.polaris.config.address", polarisConfigAddress);
		}

		// polaris_admin_port
		String polarisAdminPort = environment.getProperty("polaris_admin_port");
		if (StringUtils.isNotBlank(polarisAdminPort)) {
			polarisEnvProperties.put("spring.cloud.polaris.admin.port", polarisAdminPort);
		}

		// application_version
		String applicationVersion = environment.getProperty("tsf_prog_version");
		if (StringUtils.isNotBlank(applicationVersion)) {
			polarisEnvProperties.put("spring.cloud.polaris.discovery.version", applicationVersion);
		}

		// region
		String region = environment.getProperty("tsf_region");
		if (StringUtils.isNotBlank(region)) {
			polarisEnvProperties.put("spring.cloud.tencent.metadata.content.region", region);
		}

		// zone
		String zone = environment.getProperty("tsf_zone");
		if (StringUtils.isNotBlank(zone)) {
			polarisEnvProperties.put("spring.cloud.tencent.metadata.content.zone", zone);
		}

		// global namespace enabled
		String globalNamespaceEnabled = environment.getProperty("global_namespace_enabled");
		if (StringUtils.isNotBlank(globalNamespaceEnabled) && StringUtils.equals("true", globalNamespaceEnabled)) {
			polarisEnvProperties.put("spring.cloud.polaris.discovery.namespace-exports", "*");
		}

		MapPropertySource propertySource = new MapPropertySource("polaris-env-properties", polarisEnvProperties);
		environment.getPropertySources().addFirst(propertySource);
	}
}
