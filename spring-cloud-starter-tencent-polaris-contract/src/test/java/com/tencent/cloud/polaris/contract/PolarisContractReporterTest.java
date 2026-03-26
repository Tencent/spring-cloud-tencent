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

package com.tencent.cloud.polaris.contract;

import java.lang.reflect.Method;
import java.util.List;

import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.contract.config.PolarisContractProperties;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.plugin.server.InterfaceDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springdoc.core.providers.ObjectMapperProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisContractReporter}.
 *
 * @author Haotian Zhang
 */
@DisplayName("PolarisContractReporter")
@ExtendWith(MockitoExtension.class)
class PolarisContractReporterTest {

	@Mock
	private PolarisContractProperties polarisContractProperties;

	@Mock
	private ProviderAPI providerAPI;

	@Mock
	private PolarisDiscoveryProperties polarisDiscoveryProperties;

	@Mock
	private ObjectMapperProvider springdocObjectMapperProvider;

	private OpenAPI createTestOpenAPI() {
		OpenAPI openAPI = new OpenAPI();
		Paths paths = new Paths();

		PathItem sumPath = new PathItem();
		Operation sumGet = new Operation();
		sumGet.setSummary("sum");
		sumPath.setGet(sumGet);
		paths.addPathItem("/quickstart/callee/sum", sumPath);

		PathItem infoPath = new PathItem();
		Operation infoGet = new Operation();
		infoGet.setSummary("info");
		infoPath.setGet(infoGet);
		paths.addPathItem("/quickstart/callee/info", infoPath);

		openAPI.setPaths(paths);
		return openAPI;
	}

	@SuppressWarnings("unchecked")
	private List<InterfaceDescriptor> invokeGetInterfaceDescriptorFromSwagger(
			PolarisContractReporter reporter, OpenAPI openAPI) throws Exception {
		Method method = PolarisContractReporter.class.getDeclaredMethod(
				"getInterfaceDescriptorFromSwagger", OpenAPI.class);
		method.setAccessible(true);
		return (List<InterfaceDescriptor>) method.invoke(reporter, openAPI);
	}

	/**
	 * Test that context-path is prepended to InterfaceDescriptor paths.
	 */
	@DisplayName("context-path is prepended to interface descriptor paths")
	@Test
	void testContextPathPrependedToInterfaceDescriptorPaths() throws Exception {
		// Arrange
		String contextPath = "/callee-service";
		PolarisContractReporter reporter = new PolarisContractReporter(
				null, null, polarisContractProperties, providerAPI,
				polarisDiscoveryProperties, springdocObjectMapperProvider, contextPath);
		OpenAPI openAPI = createTestOpenAPI();

		// Act
		List<InterfaceDescriptor> descriptors = invokeGetInterfaceDescriptorFromSwagger(reporter, openAPI);

		// Assert
		assertThat(descriptors).hasSize(2);
		assertThat(descriptors)
				.extracting(InterfaceDescriptor::getPath)
				.containsExactlyInAnyOrder(
						"/callee-service/quickstart/callee/sum",
						"/callee-service/quickstart/callee/info");
	}

	/**
	 * Test that empty context-path leaves paths unchanged.
	 */
	@DisplayName("empty context-path leaves paths unchanged")
	@Test
	void testEmptyContextPathLeavesPathsUnchanged() throws Exception {
		// Arrange
		String contextPath = "";
		PolarisContractReporter reporter = new PolarisContractReporter(
				null, null, polarisContractProperties, providerAPI,
				polarisDiscoveryProperties, springdocObjectMapperProvider, contextPath);
		OpenAPI openAPI = createTestOpenAPI();

		// Act
		List<InterfaceDescriptor> descriptors = invokeGetInterfaceDescriptorFromSwagger(reporter, openAPI);

		// Assert
		assertThat(descriptors).hasSize(2);
		assertThat(descriptors)
				.extracting(InterfaceDescriptor::getPath)
				.containsExactlyInAnyOrder(
						"/quickstart/callee/sum",
						"/quickstart/callee/info");
	}

	/**
	 * Test that trailing slash on context-path is handled correctly via auto-config normalization.
	 * The auto-config strips trailing slashes before passing to the constructor,
	 * so we verify the reporter works correctly when contextPath has no trailing slash.
	 */
	@DisplayName("context-path without trailing slash works correctly")
	@Test
	void testContextPathWithoutTrailingSlash() throws Exception {
		// Arrange
		String contextPath = "/api";
		PolarisContractReporter reporter = new PolarisContractReporter(
				null, null, polarisContractProperties, providerAPI,
				polarisDiscoveryProperties, springdocObjectMapperProvider, contextPath);
		OpenAPI openAPI = createTestOpenAPI();

		// Act
		List<InterfaceDescriptor> descriptors = invokeGetInterfaceDescriptorFromSwagger(reporter, openAPI);

		// Assert
		assertThat(descriptors).hasSize(2);
		assertThat(descriptors)
				.extracting(InterfaceDescriptor::getPath)
				.allMatch(path -> path.startsWith("/api/"));
	}
}
