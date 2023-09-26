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

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.contract.PolarisContractReporter;
import com.tencent.cloud.polaris.contract.PolarisSwaggerApplicationListener;
import com.tencent.cloud.polaris.contract.filter.ApiDocServletFilter;
import com.tencent.cloud.polaris.contract.filter.ApiDocWebFluxFilter;
import com.tencent.cloud.polaris.contract.utils.PackageUtil;
import springfox.boot.starter.autoconfigure.OpenApiAutoConfiguration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.mappers.ServiceModelToSwagger2Mapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto configuration for Polaris swagger.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
@ConditionalOnProperty(name = "spring.cloud.polaris.contract.enabled", havingValue = "true", matchIfMissing = true)
@Import(OpenApiAutoConfiguration.class)
public class PolarisSwaggerAutoConfiguration {

	static {
		// After springboot2.6.x, the default path matching strategy of spring MVC is changed from ANT_PATH_MATCHER
		// mode to PATH_PATTERN_PARSER mode, causing an error. The solution is to switch to the original ANT_PATH_MATCHER mode.
		System.setProperty("spring.mvc.pathmatch.matching-strategy", "ant-path-matcher");
	}

	@Bean
	public Docket polarisDocket(PolarisContractProperties polarisContractProperties) {
		List<Predicate<String>> excludePathList = PackageUtil.getExcludePathPredicates(polarisContractProperties.getExcludePath());
		List<Predicate<String>> basePathList = PackageUtil.getBasePathPredicates(polarisContractProperties.getBasePath());
		String basePackage = PackageUtil.scanPackage(polarisContractProperties.getBasePackage());

		Predicate<String> basePathListOr = null;
		for (Predicate<String> basePathPredicate : basePathList) {
			if (basePathListOr == null) {
				basePathListOr = basePathPredicate;
			}
			else {
				basePathListOr = basePathListOr.or(basePathPredicate);
			}
		}

		Predicate<String> excludePathListOr = null;
		for (Predicate<String> excludePathPredicate : excludePathList) {
			if (excludePathListOr == null) {
				excludePathListOr = excludePathPredicate;
			}
			else {
				excludePathListOr = excludePathListOr.or(excludePathPredicate);
			}
		}

		Predicate<String> pathsPredicate = basePathListOr;

		if (excludePathListOr != null) {
			excludePathListOr = excludePathListOr.negate();
			pathsPredicate = pathsPredicate.and(excludePathListOr);
		}

		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(PackageUtil.basePackage(basePackage))
				.paths(pathsPredicate)
				.build()
				.groupName(polarisContractProperties.getGroup())
				.enable(polarisContractProperties.isEnabled())
				.directModelSubstitute(LocalDate.class, Date.class)
				.apiInfo(new ApiInfoBuilder()
						.title("Polaris Swagger API")
						.description("This is to show polaris api description.")
						.license("BSD-3-Clause")
						.licenseUrl("https://opensource.org/licenses/BSD-3-Clause")
						.termsOfServiceUrl("")
						.version("1.0.0")
						.contact(new Contact("", "", ""))
						.build());
	}

	@Bean
	@ConditionalOnBean(Docket.class)
	@ConditionalOnMissingBean
	public PolarisContractReporter polarisContractReporter(DocumentationCache documentationCache,
			ServiceModelToSwagger2Mapper swagger2Mapper, PolarisContractProperties polarisContractProperties,
			PolarisSDKContextManager polarisSDKContextManager, PolarisDiscoveryProperties polarisDiscoveryProperties) {
		return new PolarisContractReporter(documentationCache, swagger2Mapper, polarisContractProperties.getGroup(),
				polarisSDKContextManager.getProviderAPI(), polarisDiscoveryProperties);
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
