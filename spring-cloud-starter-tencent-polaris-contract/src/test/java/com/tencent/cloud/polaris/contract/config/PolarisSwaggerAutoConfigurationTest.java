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

import java.util.List;

import com.tencent.cloud.polaris.contract.SwaggerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisSwaggerAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisSwaggerAutoConfiguration")
class PolarisSwaggerAutoConfigurationTest {

	private static final String MAIN_CLASS_KEY = "$MainClass";
	private static final String PACKAGE_A = "com.tencent.cloud.tsf.demo.provider";
	private static final String PACKAGE_B = "com.tencent.cloud.tsf";

	private PolarisSwaggerAutoConfiguration autoConfiguration;
	private PolarisContractProperties properties;

	@BeforeEach
	void setUp() {
		autoConfiguration = new PolarisSwaggerAutoConfiguration();
		properties = new PolarisContractProperties();
		properties.setGroup("polaris");
		// No externally-configured base-package; drive scan purely from @SpringBootApplication.
		properties.setBasePackage(null);
	}

	/**
	 * Reproduce the reported defect: when the application declares
	 * {@code @SpringBootApplication(scanBasePackages = {pkgA, pkgB})}, the generated
	 * {@link GroupedOpenApi} must expose the two packages as independent entries so
	 * springdoc can scan controllers in BOTH packages. Before the fix, the comma-joined
	 * string was passed as a single package name, causing springdoc to match nothing
	 * and the Polaris / TSF console API list to be empty.
	 */
	@DisplayName("multi-value scanBasePackages produces independent packagesToScan entries")
	@Test
	void testMultiValueScanBasePackagesSplitIntoIndependentEntries() {
		// Arrange
		SwaggerContext.setAttribute(MAIN_CLASS_KEY, MultiPackageApplication.class);

		// Act
		GroupedOpenApi groupedOpenApi = autoConfiguration.polarisGroupedOpenApi(properties);

		// Assert
		List<String> packagesToScan = groupedOpenApi.getPackagesToScan();
		assertThat(packagesToScan)
				.as("each scanBasePackages value must land as its own entry, not a comma-joined string")
				.containsExactlyInAnyOrder(PACKAGE_A, PACKAGE_B);
	}

	/**
	 * Regression guard for the single-value case: keep behavior identical to before
	 * the fix when only one package is declared.
	 */
	@DisplayName("single-value scanBasePackages keeps the sole package entry")
	@Test
	void testSingleValueScanBasePackages() {
		// Arrange
		SwaggerContext.setAttribute(MAIN_CLASS_KEY, SinglePackageApplication.class);

		// Act
		GroupedOpenApi groupedOpenApi = autoConfiguration.polarisGroupedOpenApi(properties);

		// Assert
		assertThat(groupedOpenApi.getPackagesToScan()).containsExactly(PACKAGE_A);
	}

	/**
	 * Externally-configured {@code spring.cloud.polaris.contract.base-package=a,b}
	 * must also be split into independent entries.
	 */
	@DisplayName("comma-separated contract base-package property splits into independent entries")
	@Test
	void testCommaSeparatedBasePackageProperty() {
		// Arrange
		SwaggerContext.setAttribute(MAIN_CLASS_KEY, SinglePackageApplication.class);
		properties.setBasePackage(PACKAGE_A + "," + PACKAGE_B);

		// Act
		GroupedOpenApi groupedOpenApi = autoConfiguration.polarisGroupedOpenApi(properties);

		// Assert
		assertThat(groupedOpenApi.getPackagesToScan())
				.containsExactlyInAnyOrder(PACKAGE_A, PACKAGE_B);
	}

	@SpringBootApplication(scanBasePackages = {PACKAGE_A, PACKAGE_B})
	private static class MultiPackageApplication {
	}

	@SpringBootApplication(scanBasePackages = {PACKAGE_A})
	private static class SinglePackageApplication {
	}
}
