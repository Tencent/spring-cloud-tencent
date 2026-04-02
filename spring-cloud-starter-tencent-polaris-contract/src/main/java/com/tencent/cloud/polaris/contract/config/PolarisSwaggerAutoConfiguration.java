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

package com.tencent.cloud.polaris.contract.config;

import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.contract.PolarisContractReporter;
import com.tencent.cloud.polaris.contract.PolarisSwaggerApplicationListener;
import com.tencent.cloud.polaris.contract.filter.ApiDocServletFilter;
import com.tencent.cloud.polaris.contract.filter.ApiDocWebFluxFilter;
import com.tencent.cloud.polaris.contract.utils.PackageUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.SpringDocConfiguration;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webflux.api.MultipleOpenApiWebFluxResource;
import org.springdoc.webmvc.api.MultipleOpenApiWebMvcResource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import static com.tencent.cloud.polaris.contract.utils.PackageUtil.SPLITTER;

/**
 * Auto configuration for Polaris swagger.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
@ConditionalOnProperty(name = "spring.cloud.polaris.contract.enabled", havingValue = "true", matchIfMissing = true)
@Import(SpringDocConfiguration.class)
public class PolarisSwaggerAutoConfiguration {

	static {
		// After springboot2.6.x, the default path matching strategy of spring MVC is changed from ANT_PATH_MATCHER
		// mode to PATH_PATTERN_PARSER mode, causing an error. The solution is to switch to the original ANT_PATH_MATCHER mode.
		System.setProperty("spring.mvc.pathmatch.matching-strategy", "ant-path-matcher");
	}

	@Bean
	public GroupedOpenApi polarisGroupedOpenApi(PolarisContractProperties polarisContractProperties) {
		String basePackage = PackageUtil.scanPackage(polarisContractProperties.getBasePackage());
		String[] basePaths = {};
		if (StringUtils.hasText(polarisContractProperties.getBasePath())) {
			basePaths = polarisContractProperties.getBasePath().split(SPLITTER);
		}
		String[] excludePaths = {};
		if (StringUtils.hasText(polarisContractProperties.getExcludePath())) {
			excludePaths = polarisContractProperties.getExcludePath().split(SPLITTER);
		}
		return GroupedOpenApi.builder()
				.packagesToScan(basePackage)
				.pathsToMatch(basePaths)
				.pathsToExclude(excludePaths)
				.group(polarisContractProperties.getGroup())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public OpenAPI polarisOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Polaris Contract")
						.description("This is to show polaris contract description.")
						.license(new License().name("BSD-3-Clause").url("https://opensource.org/licenses/BSD-3-Clause"))
						.version("1.0.0"));
	}

	@Bean
	@ConditionalOnBean(OpenAPI.class)
	@ConditionalOnMissingBean
	public PolarisContractReporter polarisContractReporter(
			@Nullable MultipleOpenApiWebMvcResource multipleOpenApiWebMvcResource,
			@Nullable MultipleOpenApiWebFluxResource multipleOpenApiWebFluxResource,
			PolarisContractProperties polarisContractProperties, PolarisSDKContextManager polarisSDKContextManager,
			PolarisDiscoveryProperties polarisDiscoveryProperties, ObjectMapperProvider springdocObjectMapperProvider,
			Environment environment) {
		String contextPath = environment.getProperty("server.servlet.context-path", "");
		if (!StringUtils.hasText(contextPath)) {
			contextPath = environment.getProperty("spring.webflux.base-path", "");
		}
		if (contextPath.endsWith("/")) {
			contextPath = contextPath.substring(0, contextPath.length() - 1);
		}
		return new PolarisContractReporter(multipleOpenApiWebMvcResource, multipleOpenApiWebFluxResource,
				polarisContractProperties, polarisSDKContextManager.getProviderAPI(), polarisDiscoveryProperties,
				springdocObjectMapperProvider, contextPath);
	}

	@Bean
	@ConditionalOnMissingBean
	public PolarisSwaggerApplicationListener polarisSwaggerApplicationListener() {
		return new PolarisSwaggerApplicationListener();
	}

	/**
	 * Create when web application type is SERVLET.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	protected static class SwaggerServletConfig {
		@Bean
		public ApiDocServletFilter apiDocServletFilter(PolarisContractProperties polarisContractProperties) {
			return new ApiDocServletFilter(polarisContractProperties);
		}
	}

	/**
	 * Create when web application type is REACTIVE.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	protected static class SwaggerReactiveConfig {

		@Bean
		public ApiDocWebFluxFilter apiDocWebFluxFilter(PolarisContractProperties polarisContractProperties) {
			return new ApiDocWebFluxFilter(polarisContractProperties);
		}
	}
}
