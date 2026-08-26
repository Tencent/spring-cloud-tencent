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

package com.tencent.cloud.polaris.config;

import com.tencent.cloud.polaris.config.adapter.SpringConfigEffectiveValueProvider;
import com.tencent.polaris.configuration.api.core.ConfigEffectiveValueRegistration;
import com.tencent.polaris.configuration.api.core.ConfigFileService;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Registers the Spring-Environment-based effective value provider into polaris-java
 * for config effective-time realtime query.
 * <p>
 * This auto-configuration is intentionally NOT registered as a BootstrapConfiguration:
 * in legacy bootstrap mode the bootstrap context's Environment only carries bootstrap.yml
 * sources, so a provider registered there would resolve stale or missing effective values.
 * Being main-context-only guarantees the provider always captures the Environment the
 * application actually reads. In legacy bootstrap mode the {@link ConfigFileService} bean
 * is inherited from the bootstrap (parent) context.
 *
 * @author evelynwei
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisConfigEnabled
@AutoConfigureAfter(PolarisConfigBootstrapAutoConfiguration.class)
public class PolarisConfigEffectiveValueAutoConfiguration {

	/**
	 * Enabled by default (aligned with the config watch report switch); set
	 * spring.cloud.polaris.config.report.effective.enabled=false to opt out. The returned
	 * registration is closed on context shutdown so the SDK never holds a destroyed
	 * Environment.
	 * <p>
	 * {@code @AutoConfigureAfter} on this class guarantees the {@code configFileService}
	 * bean definition of {@link PolarisConfigBootstrapAutoConfiguration} is already
	 * processed, so {@code @ConditionalOnBean} sees it in ConfigData mode; in legacy
	 * bootstrap mode the bean is visible in the parent context.
	 */
	@Bean(destroyMethod = "close")
	@ConditionalOnBean(ConfigFileService.class)
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.cloud.polaris.config.report.effective", name = "enabled",
			havingValue = "true", matchIfMissing = true)
	public ConfigEffectiveValueRegistration polarisConfigEffectiveValueRegistration(
			ConfigurableEnvironment environment, ConfigFileService configFileService) {
		return configFileService.registerEffectiveValueProvider(new SpringConfigEffectiveValueProvider(environment));
	}
}
