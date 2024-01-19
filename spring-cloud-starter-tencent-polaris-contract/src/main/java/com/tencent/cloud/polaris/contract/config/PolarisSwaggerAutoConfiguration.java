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
import org.springdoc.webflux.api.MultipleOpenApiWebFluxResource;
import org.springdoc.webmvc.api.MultipleOpenApiWebMvcResource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
	public OpenAPI polarisOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Polaris Swagger API")
						.description("This is to show polaris api description.")
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
			PolarisDiscoveryProperties polarisDiscoveryProperties) {
		return new PolarisContractReporter(multipleOpenApiWebMvcResource, multipleOpenApiWebFluxResource,
				polarisContractProperties, polarisSDKContextManager.getProviderAPI(), polarisDiscoveryProperties);
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
