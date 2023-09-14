/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 *  Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/BSD-3-Clause
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package com.tencent.cloud.polaris.config;

import com.tencent.cloud.polaris.config.adapter.AffectedConfigurationPropertiesRebinder;
import com.tencent.cloud.polaris.config.adapter.PolarisConfigPropertyRefresher;
import com.tencent.cloud.polaris.config.adapter.PolarisConfigRefreshScopeAnnotationDetector;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.adapter.PolarisRefreshAffectedContextRefresher;
import com.tencent.cloud.polaris.config.adapter.PolarisRefreshEntireContextRefresher;
import com.tencent.cloud.polaris.config.annotation.PolarisConfigAnnotationProcessor;
import com.tencent.cloud.polaris.config.condition.ConditionalOnReflectRefreshType;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.listener.PolarisConfigChangeEventListener;
import com.tencent.cloud.polaris.config.listener.PolarisConfigRefreshOptimizationListener;
import com.tencent.cloud.polaris.config.logger.PolarisConfigLoggerApplicationListener;
import com.tencent.cloud.polaris.config.spring.annotation.SpringValueProcessor;
import com.tencent.cloud.polaris.config.spring.property.PlaceholderHelper;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.cloud.context.properties.ConfigurationPropertiesBeans;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * polaris config module auto configuration at init application context phase.
 *
 * @author lepdou 2022-03-28
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisConfigEnabled
public class PolarisConfigAutoConfiguration {

	@Bean
	public PolarisConfigAnnotationProcessor polarisConfigAnnotationProcessor() {
		return new PolarisConfigAnnotationProcessor();
	}

	@Bean
	public PolarisConfigChangeEventListener polarisConfigChangeEventListener() {
		return new PolarisConfigChangeEventListener();
	}

	@Bean
	public PolarisConfigLoggerApplicationListener polarisConfigLoggerApplicationListener() {
		return new PolarisConfigLoggerApplicationListener();
	}

	@Bean
	@Primary
	@ConditionalOnReflectRefreshType
	public ConfigurationPropertiesRebinder affectedConfigurationPropertiesRebinder(
			ConfigurationPropertiesBeans beans) {
		return new AffectedConfigurationPropertiesRebinder(beans);
	}

	@Bean
	@ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
	public PolarisConfigPropertyRefresher polarisRefreshContextPropertySourceAutoRefresher(PolarisConfigProperties polarisConfigProperties,
			PolarisPropertySourceManager polarisPropertySourceManager, ContextRefresher contextRefresher) {
		return new PolarisRefreshEntireContextRefresher(polarisConfigProperties, polarisPropertySourceManager, contextRefresher);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnReflectRefreshType
	@AutoConfigureBefore(PolarisConfigAutoConfiguration.class)
	public static class PolarisReflectRefresherAutoConfiguration {
		@Bean
		public SpringValueRegistry springValueRegistry() {
			return new SpringValueRegistry();
		}

		@Bean
		public PlaceholderHelper placeholderHelper() {
			return new PlaceholderHelper();
		}

		@Bean
		public SpringValueProcessor springValueProcessor(PlaceholderHelper placeholderHelper,
				SpringValueRegistry springValueRegistry, PolarisConfigProperties polarisConfigProperties) {
			return new SpringValueProcessor(placeholderHelper, springValueRegistry, polarisConfigProperties);
		}

		@Bean
		public PolarisConfigPropertyRefresher polarisReflectPropertySourceAutoRefresher(PolarisConfigProperties polarisConfigProperties,
				PolarisPropertySourceManager polarisPropertySourceManager, SpringValueRegistry springValueRegistry,
				PlaceholderHelper placeholderHelper) {
			return new PolarisRefreshAffectedContextRefresher(polarisConfigProperties, polarisPropertySourceManager,
					springValueRegistry, placeholderHelper);
		}

		@Bean
		public PolarisConfigRefreshScopeAnnotationDetector polarisConfigRefreshScopeAnnotationDetector() {
			return new PolarisConfigRefreshScopeAnnotationDetector();
		}

		@Bean
		public PolarisConfigRefreshOptimizationListener polarisConfigRefreshOptimizationListener() {
			return new PolarisConfigRefreshOptimizationListener();
		}
	}
}
