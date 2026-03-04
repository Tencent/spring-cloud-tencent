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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tencent.cloud.common.util.AddressUtils;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.config.PolarisCryptoConfigProperties;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

/**
* Test for {@link ConfigurationModifier}.
*
* @author Haotian Zhang
*/
@DisplayName("ConfigurationModifier Test")
@ExtendWith(MockitoExtension.class)
class ConfigurationModifierTest {

	@Mock
	private PolarisConfigProperties polarisConfigProperties;

	@Mock
	private PolarisCryptoConfigProperties polarisCryptoConfigProperties;

	@Mock
	private PolarisContextProperties polarisContextProperties;

	private ConfigurationModifier configurationModifier;

	@BeforeEach
	void setUp() {
		configurationModifier = new ConfigurationModifier(
				polarisConfigProperties, polarisCryptoConfigProperties, polarisContextProperties);
	}

	/**
	* Invoke the private method resolvePolarisAddressFromConfigAddress via reflection.
	*/
	@SuppressWarnings("unchecked")
	private List<String> invokeResolvePolarisAddressFromConfigAddress(String configAddress) throws Exception {
		Method method = ConfigurationModifier.class.getDeclaredMethod(
				"resolvePolarisAddressFromConfigAddress", String.class);
		method.setAccessible(true);
		return (List<String>) method.invoke(configurationModifier, configAddress);
	}

	/**
	* Test resolvePolarisAddressFromConfigAddress when configAddress is null.
	* Scenario: pass null as configAddress.
	* Expect: return null directly without calling AddressUtils.
	*/
	@DisplayName("resolvePolarisAddressFromConfigAddress should return null when configAddress is null")
	@Test
	void testResolvePolarisAddressFromConfigAddress_Null() throws Exception {
		// Act
		List<String> result = invokeResolvePolarisAddressFromConfigAddress(null);

		// Assert
		assertThat(result).isNull();
	}

	/**
	* Test resolvePolarisAddressFromConfigAddress when configAddress is empty string.
	* Scenario: pass empty string as configAddress.
	* Expect: return null directly without calling AddressUtils.
	*/
	@DisplayName("resolvePolarisAddressFromConfigAddress should return null when configAddress is empty")
	@Test
	void testResolvePolarisAddressFromConfigAddress_Empty() throws Exception {
		// Act
		List<String> result = invokeResolvePolarisAddressFromConfigAddress("");

		// Assert
		assertThat(result).isNull();
	}

	/**
	* Test resolvePolarisAddressFromConfigAddress with a single address containing port.
	* Scenario: pass a single config address "grpc://192.168.1.100:8093".
	* Expect: the port should be replaced with 8091.
	*/
	@DisplayName("resolvePolarisAddressFromConfigAddress should replace port with 8091 for single address")
	@Test
	void testResolvePolarisAddressFromConfigAddress_SingleAddress() throws Exception {
		// Arrange
		String configAddress = "grpc://192.168.1.100:8093";

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Collections.singletonList("192.168.1.100:8093"));

			// Act
			List<String> result = invokeResolvePolarisAddressFromConfigAddress(configAddress);

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(1);
			assertThat(result.get(0)).isEqualTo("192.168.1.100:8091");
		}
	}

	/**
	* Test resolvePolarisAddressFromConfigAddress with multiple addresses containing ports.
	* Scenario: pass multiple config addresses separated by comma.
	* Expect: each address port should be replaced with 8091.
	*/
	@DisplayName("resolvePolarisAddressFromConfigAddress should replace port with 8091 for multiple addresses")
	@Test
	void testResolvePolarisAddressFromConfigAddress_MultipleAddresses() throws Exception {
		// Arrange
		String configAddress = "grpc://10.0.1.1:8093,grpc://10.0.1.2:8094";

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Arrays.asList("10.0.1.1:8093", "10.0.1.2:8094"));

			// Act
			List<String> result = invokeResolvePolarisAddressFromConfigAddress(configAddress);

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(2);
			assertThat(result.get(0)).isEqualTo("10.0.1.1:8091");
			assertThat(result.get(1)).isEqualTo("10.0.1.2:8091");
		}
	}

	/**
	* Test resolvePolarisAddressFromConfigAddress with address that has no port (no colon).
	* Scenario: AddressUtils returns an address without colon separator.
	* Expect: the address should be kept as-is.
	*/
	@DisplayName("resolvePolarisAddressFromConfigAddress should keep address as-is when no port")
	@Test
	void testResolvePolarisAddressFromConfigAddress_AddressWithoutPort() throws Exception {
		// Arrange
		String configAddress = "grpc://localhost";

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Collections.singletonList("localhost"));

			// Act
			List<String> result = invokeResolvePolarisAddressFromConfigAddress(configAddress);

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(1);
			assertThat(result.get(0)).isEqualTo("localhost");
		}
	}

	/**
	* Test resolvePolarisAddressFromConfigAddress with blank address in the list.
	* Scenario: AddressUtils returns a list containing blank strings.
	* Expect: blank addresses should be skipped.
	*/
	@DisplayName("resolvePolarisAddressFromConfigAddress should skip blank addresses")
	@Test
	void testResolvePolarisAddressFromConfigAddress_BlankAddressSkipped() throws Exception {
		// Arrange
		String configAddress = "grpc://172.16.0.1:8093, ,grpc://172.16.0.2:9090";

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Arrays.asList("172.16.0.1:8093", " ", "172.16.0.2:9090"));

			// Act
			List<String> result = invokeResolvePolarisAddressFromConfigAddress(configAddress);

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(2);
			assertThat(result.get(0)).isEqualTo("172.16.0.1:8091");
			assertThat(result.get(1)).isEqualTo("172.16.0.2:8091");
		}
	}

	/**
	* Test resolvePolarisAddressFromConfigAddress with IPv6 address containing port.
	* Scenario: pass an IPv6 config address like "grpc://[::1]:8093".
	* Expect: the port should be replaced with 8091, preserving the IPv6 format.
	*/
	@DisplayName("resolvePolarisAddressFromConfigAddress should replace port with 8091 for IPv6 address")
	@Test
	void testResolvePolarisAddressFromConfigAddress_Ipv6Address() throws Exception {
		// Arrange
		String configAddress = "grpc://[::1]:8093";

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Collections.singletonList("[::1]:8093"));

			// Act
			List<String> result = invokeResolvePolarisAddressFromConfigAddress(configAddress);

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(1);
			assertThat(result.get(0)).isEqualTo("[::1]:8091");
		}
	}
}
