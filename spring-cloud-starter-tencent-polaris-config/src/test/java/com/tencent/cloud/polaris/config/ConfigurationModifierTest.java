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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.cloud.common.util.AddressUtils;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.config.PolarisCryptoConfigProperties;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig;
import com.tencent.polaris.configuration.client.internal.ConfigWatchReportRequestCustomizer;
import com.tencent.polaris.configuration.client.internal.ConfigWatchReportRequestCustomizerConfig;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.configuration.ConfigFileConfigImpl;
import com.tencent.polaris.factory.config.configuration.ConfigFilterConfigImpl;
import com.tencent.polaris.factory.config.configuration.ConnectorConfigImpl;
import com.tencent.polaris.factory.config.consumer.CircuitBreakerConfigImpl;
import com.tencent.polaris.factory.config.consumer.ConsumerConfigImpl;
import com.tencent.polaris.factory.config.consumer.OutlierDetectionConfigImpl;
import com.tencent.polaris.factory.config.global.APIConfigImpl;
import com.tencent.polaris.factory.config.global.GlobalConfigImpl;
import com.tencent.polaris.factory.config.global.ReportClientRequestCustomizerConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.config.global.StatReporterConfigImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ConfigurationModifier}.
 *
 * @author Haotian Zhang
 */
@DisplayName("ConfigurationModifier")
@ExtendWith(MockitoExtension.class)
class ConfigurationModifierTest {

	@Mock
	private PolarisConfigProperties polarisConfigProperties;

	@Mock
	private PolarisCryptoConfigProperties polarisCryptoConfigProperties;

	@Mock
	private PolarisConfigProperties.Report report;

	@Mock
	private PolarisContextProperties polarisContextProperties;

	@Mock
	private ConfigWatchReportRequestCustomizerConfig configWatchCustomizer;

	private ConfigurationModifier configurationModifier;

	@BeforeEach
	void setUp() {
		configurationModifier = new ConfigurationModifier(
				polarisConfigProperties, polarisCryptoConfigProperties, polarisContextProperties);
		Mockito.lenient().when(polarisConfigProperties.getReport()).thenReturn(report);
		Mockito.lenient().when(report.isEnabled()).thenReturn(true);
	}

	/**
	 * Build a mock ConfigurationImpl with all necessary nested objects.
	 */
	private ConfigurationImpl buildMockConfiguration() {
		ConfigurationImpl configuration = mock(ConfigurationImpl.class);

		GlobalConfigImpl globalConfig = mock(GlobalConfigImpl.class);
		StatReporterConfigImpl statReporter = mock(StatReporterConfigImpl.class);
		ReportClientRequestCustomizerConfigImpl requestCustomizer =
				mock(ReportClientRequestCustomizerConfigImpl.class);
		ServerConnectorConfigImpl serverConnector = mock(ServerConnectorConfigImpl.class);
		APIConfigImpl apiConfig = mock(APIConfigImpl.class);
		when(globalConfig.getStatReporter()).thenReturn(statReporter);
		when(globalConfig.getReportClientRequestCustomizer()).thenReturn(requestCustomizer);
		when(requestCustomizer.getPluginConfig(ConfigWatchReportRequestCustomizer.NAME,
				ConfigWatchReportRequestCustomizerConfig.class)).thenReturn(configWatchCustomizer);
		Mockito.lenient().when(globalConfig.getServerConnector()).thenReturn(serverConnector);
		Mockito.lenient().when(globalConfig.getAPI()).thenReturn(apiConfig);
		when(configuration.getGlobal()).thenReturn(globalConfig);

		ConsumerConfigImpl consumerConfig = mock(ConsumerConfigImpl.class);
		OutlierDetectionConfigImpl outlierDetection = mock(OutlierDetectionConfigImpl.class);
		CircuitBreakerConfigImpl circuitBreaker = mock(CircuitBreakerConfigImpl.class);
		when(consumerConfig.getOutlierDetection()).thenReturn(outlierDetection);
		when(consumerConfig.getCircuitBreaker()).thenReturn(circuitBreaker);
		when(configuration.getConsumer()).thenReturn(consumerConfig);

		ConfigFileConfigImpl configFileConfig = mock(ConfigFileConfigImpl.class);
		ConnectorConfigImpl connectorConfig = mock(ConnectorConfigImpl.class);
		ConfigFilterConfigImpl configFilterConfig = mock(ConfigFilterConfigImpl.class);
		Mockito.lenient().when(configFilterConfig.getChain()).thenReturn(new ArrayList<>());
		Mockito.lenient().when(configFilterConfig.getPlugin()).thenReturn(new HashMap<>());
		Mockito.lenient().when(configFileConfig.getServerConnector()).thenReturn(connectorConfig);
		Mockito.lenient().when(configFileConfig.getConfigFilterConfig()).thenReturn(configFilterConfig);
		when(configuration.getConfigFile()).thenReturn(configFileConfig);

		return configuration;
	}

	/**
	 * Test getOrder returns CONFIG_ORDER.
	 * Scenario: call getOrder method.
	 * Expect: return value equals OrderConstant.Modifier.CONFIG_ORDER.
	 */
	@DisplayName("getOrder should return CONFIG_ORDER")
	@Test
	void testGetOrder() {
		// Act
		int order = configurationModifier.getOrder();

		// Assert
		assertThat(order).isEqualTo(OrderConstant.Modifier.CONFIG_ORDER);
	}

	/**
	 * Test modify when polaris context is disabled.
	 * Scenario: polarisContextProperties.getEnabled() returns false.
	 * Expect: stat reporter is disabled, outlier detection set to never, circuit breaker disabled,
	 *         but initDataSource is not called.
	 */
	@DisplayName("modify should return early when polaris context is disabled")
	@Test
	void testModify_PolarisContextDisabled() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(false);

		// Act
		configurationModifier.modify(configuration);

		// Assert
		verify(configuration.getGlobal().getStatReporter()).setEnable(false);
		verify(configuration.getGlobal().getReportClientRequestCustomizer()
				.getPluginConfig(ConfigWatchReportRequestCustomizer.NAME,
						ConfigWatchReportRequestCustomizerConfig.class)).setEnable(true);
		verify(configuration.getGlobal().getReportClientRequestCustomizer())
				.setPluginConfig(ConfigWatchReportRequestCustomizer.NAME, configWatchCustomizer);
		verify(configuration.getConsumer().getOutlierDetection()).setWhen(OutlierDetectionConfig.When.never);
		verify(configuration.getConsumer().getCircuitBreaker()).setEnable(false);
		verify(configuration.getConfigFile(), never()).getServerConnector();
	}

	/**
	 * Test modify when config is disabled.
	 * Scenario: polarisContextProperties.getEnabled() returns true but polarisConfigProperties.isEnabled() returns false.
	 * Expect: early return without calling initDataSource.
	 */
	@DisplayName("modify should return early when config is disabled")
	@Test
	void testModify_ConfigDisabled() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(false);

		// Act
		configurationModifier.modify(configuration);

		// Assert
		verify(configuration.getGlobal().getStatReporter()).setEnable(false);
		verify(configuration.getGlobal().getReportClientRequestCustomizer()
				.getPluginConfig(ConfigWatchReportRequestCustomizer.NAME,
						ConfigWatchReportRequestCustomizerConfig.class)).setEnable(true);
		verify(configuration.getGlobal().getReportClientRequestCustomizer())
				.setPluginConfig(ConfigWatchReportRequestCustomizer.NAME, configWatchCustomizer);
		verify(configuration.getConfigFile(), never()).getServerConnector();
	}

	@DisplayName("modify should disable config watch reporter when configured")
	@Test
	void testModify_ConfigWatchReportDisabled() {
		ConfigurationImpl configuration = buildMockConfiguration();
		when(report.isEnabled()).thenReturn(false);
		when(polarisContextProperties.getEnabled()).thenReturn(false);

		configurationModifier.modify(configuration);

		verify(configWatchCustomizer).setEnable(false);
		verify(configuration.getGlobal().getReportClientRequestCustomizer())
				.setPluginConfig(ConfigWatchReportRequestCustomizer.NAME, configWatchCustomizer);
		verify(configuration.getConfigFile(), never()).getServerConnector();
	}

	/**
	 * Test modify with local file data source.
	 * Scenario: dataSource is "localFile", both polaris and config are enabled.
	 * Expect: connector type is set to localFile and persistDir is set.
	 */
	@DisplayName("modify should configure local file data source")
	@Test
	void testModify_LocalFileDataSource() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("local");
		when(polarisConfigProperties.getLocalFileRootPath()).thenReturn("/tmp/config");
		when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);

		// Act
		configurationModifier.modify(configuration);

		// Assert
		ConnectorConfigImpl connectorConfig = configuration.getConfigFile().getServerConnector();
		verify(connectorConfig).setConnectorType("local");
		verify(connectorConfig).setPersistDir("/tmp/config");
	}

	/**
	 * Test modify with polaris data source using explicit config address.
	 * Scenario: polarisConfigProperties.getAddress() returns a valid address.
	 * Expect: connectorConfig addresses are set from config address.
	 */
	@DisplayName("modify should configure polaris data source with explicit config address")
	@Test
	void testModify_PolarisDataSourceWithConfigAddress() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn("grpc://127.0.0.1:8093");
		when(polarisConfigProperties.isCheckAddress()).thenReturn(false);
		when(polarisConfigProperties.isEmptyProtectionEnabled()).thenReturn(true);
		when(polarisConfigProperties.getEmptyProtectionExpiredInterval()).thenReturn(604800000L);
		when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);
		when(polarisContextProperties.getAddress()).thenReturn("grpc://127.0.0.1:8091");
		when(polarisContextProperties.getAddressLbPolicy()).thenReturn("roundRobin");
		when(polarisContextProperties.getServerSwitchInterval()).thenReturn(600000L);

		List<String> parsedAddresses = Collections.singletonList("127.0.0.1:8093");
		List<String> parsedPolarisAddresses = Collections.singletonList("127.0.0.1:8091");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class);
				MockedStatic<TsfContextUtils> mockedTsf = Mockito.mockStatic(TsfContextUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8093"))
					.thenReturn(parsedAddresses);
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8091"))
					.thenReturn(parsedPolarisAddresses);
			mockedTsf.when(TsfContextUtils::isOnlyTsfConsulEnabled).thenReturn(false);

			// Act
			configurationModifier.modify(configuration);

			// Assert
			ConnectorConfigImpl connectorConfig = configuration.getConfigFile().getServerConnector();
			verify(connectorConfig).setAddresses(parsedAddresses);
			verify(connectorConfig).setLbPolicy("roundRobin");
			verify(connectorConfig).setServerSwitchInterval(600000L);
			verify(connectorConfig).setEmptyProtectionEnable(true);
			verify(connectorConfig).setEmptyProtectionExpiredInterval(604800000L);
		}
	}

	/**
	 * Test modify with polaris data source falling back to polaris address.
	 * Scenario: polarisConfigProperties.getAddress() is empty, fallback to polarisContextProperties.getAddress().
	 * Expect: config addresses are resolved from polaris address with replaced port.
	 */
	@DisplayName("modify should resolve config address from polaris address when config address is empty")
	@Test
	void testModify_PolarisDataSourceWithPolarisAddressFallback() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn(null);
		when(polarisConfigProperties.getPort()).thenReturn(8093);
		when(polarisConfigProperties.isCheckAddress()).thenReturn(false);
		when(polarisConfigProperties.isEmptyProtectionEnabled()).thenReturn(true);
		when(polarisConfigProperties.getEmptyProtectionExpiredInterval()).thenReturn(604800000L);
		when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);
		when(polarisContextProperties.getAddress()).thenReturn("grpc://127.0.0.1:8091");
		when(polarisContextProperties.getAddressLbPolicy()).thenReturn("roundRobin");
		when(polarisContextProperties.getServerSwitchInterval()).thenReturn(600000L);

		List<String> parsedPolarisAddresses = Collections.singletonList("127.0.0.1:8091");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class);
				MockedStatic<TsfContextUtils> mockedTsf = Mockito.mockStatic(TsfContextUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8091"))
					.thenReturn(parsedPolarisAddresses);
			mockedTsf.when(TsfContextUtils::isOnlyTsfConsulEnabled).thenReturn(false);

			// Act
			configurationModifier.modify(configuration);

			// Assert
			ConnectorConfigImpl connectorConfig = configuration.getConfigFile().getServerConnector();
			verify(connectorConfig).setAddresses(Collections.singletonList("127.0.0.1:8093"));
		}
	}

	/**
	 * Test modify throws RuntimeException when config addresses are empty.
	 * Scenario: both config address and polaris address are empty.
	 * Expect: RuntimeException is thrown.
	 */
	@DisplayName("modify should throw RuntimeException when config addresses are empty")
	@Test
	void testModify_EmptyConfigAddressesThrowsException() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn(null);
		when(polarisContextProperties.getAddress()).thenReturn(null);

		// Act & Assert
		assertThatThrownBy(() -> configurationModifier.modify(configuration))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Config server address is blank");
	}

	/**
	 * Test modify with crypto enabled.
	 * Scenario: polarisCryptoConfigProperties.isEnabled() returns true.
	 * Expect: configFilterConfig is enabled and crypto chain/plugin are added.
	 */
	@DisplayName("modify should enable crypto config filter when crypto is enabled")
	@Test
	void testModify_CryptoEnabled() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn("grpc://127.0.0.1:8093");
		when(polarisConfigProperties.isCheckAddress()).thenReturn(false);
		when(polarisConfigProperties.isEmptyProtectionEnabled()).thenReturn(true);
		when(polarisConfigProperties.getEmptyProtectionExpiredInterval()).thenReturn(604800000L);
		when(polarisCryptoConfigProperties.isEnabled()).thenReturn(true);
		when(polarisContextProperties.getAddress()).thenReturn("grpc://127.0.0.1:8091");
		when(polarisContextProperties.getAddressLbPolicy()).thenReturn("roundRobin");
		when(polarisContextProperties.getServerSwitchInterval()).thenReturn(600000L);

		List<String> parsedAddresses = Collections.singletonList("127.0.0.1:8093");
		List<String> parsedPolarisAddresses = Collections.singletonList("127.0.0.1:8091");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class);
				MockedStatic<TsfContextUtils> mockedTsf = Mockito.mockStatic(TsfContextUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8093"))
					.thenReturn(parsedAddresses);
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8091"))
					.thenReturn(parsedPolarisAddresses);
			mockedTsf.when(TsfContextUtils::isOnlyTsfConsulEnabled).thenReturn(false);

			// Act
			configurationModifier.modify(configuration);

			// Assert
			ConfigFilterConfigImpl configFilterConfig = configuration.getConfigFile().getConfigFilterConfig();
			verify(configFilterConfig).setEnable(true);
			assertThat(configFilterConfig.getChain()).contains("crypto");
			assertThat(configFilterConfig.getPlugin()).containsKey("crypto");
			Map<String, String> cryptoPlugin = (Map<String, String>) configFilterConfig.getPlugin().get("crypto");
			assertThat(cryptoPlugin).containsEntry("type", "AES");
		}
	}

	/**
	 * Test modify with crypto disabled.
	 * Scenario: polarisCryptoConfigProperties.isEnabled() returns false.
	 * Expect: configFilterConfig is disabled and crypto chain is not added.
	 */
	@DisplayName("modify should disable crypto config filter when crypto is disabled")
	@Test
	void testModify_CryptoDisabled() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn("grpc://127.0.0.1:8093");
		when(polarisConfigProperties.isCheckAddress()).thenReturn(false);
		when(polarisConfigProperties.isEmptyProtectionEnabled()).thenReturn(true);
		when(polarisConfigProperties.getEmptyProtectionExpiredInterval()).thenReturn(604800000L);
		when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);
		when(polarisContextProperties.getAddress()).thenReturn("grpc://127.0.0.1:8091");
		when(polarisContextProperties.getAddressLbPolicy()).thenReturn("roundRobin");
		when(polarisContextProperties.getServerSwitchInterval()).thenReturn(600000L);

		List<String> parsedAddresses = Collections.singletonList("127.0.0.1:8093");
		List<String> parsedPolarisAddresses = Collections.singletonList("127.0.0.1:8091");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class);
				MockedStatic<TsfContextUtils> mockedTsf = Mockito.mockStatic(TsfContextUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8093"))
					.thenReturn(parsedAddresses);
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8091"))
					.thenReturn(parsedPolarisAddresses);
			mockedTsf.when(TsfContextUtils::isOnlyTsfConsulEnabled).thenReturn(false);

			// Act
			configurationModifier.modify(configuration);

			// Assert
			ConfigFilterConfigImpl configFilterConfig = configuration.getConfigFile().getConfigFilterConfig();
			verify(configFilterConfig).setEnable(false);
			assertThat(configFilterConfig.getChain()).doesNotContain("crypto");
		}
	}

	/**
	 * Test modify with token set.
	 * Scenario: polarisConfigProperties.getToken() returns a non-empty token.
	 * Expect: connectorConfig.setToken is called.
	 */
	@DisplayName("modify should set token when token is configured")
	@Test
	void testModify_WithToken() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn("grpc://127.0.0.1:8093");
		when(polarisConfigProperties.isCheckAddress()).thenReturn(false);
		when(polarisConfigProperties.getToken()).thenReturn("test-token-123");
		when(polarisConfigProperties.isEmptyProtectionEnabled()).thenReturn(true);
		when(polarisConfigProperties.getEmptyProtectionExpiredInterval()).thenReturn(604800000L);
		when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);
		when(polarisContextProperties.getAddress()).thenReturn("grpc://127.0.0.1:8091");
		when(polarisContextProperties.getAddressLbPolicy()).thenReturn("roundRobin");
		when(polarisContextProperties.getServerSwitchInterval()).thenReturn(600000L);

		List<String> parsedAddresses = Collections.singletonList("127.0.0.1:8093");
		List<String> parsedPolarisAddresses = Collections.singletonList("127.0.0.1:8091");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class);
				MockedStatic<TsfContextUtils> mockedTsf = Mockito.mockStatic(TsfContextUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8093"))
					.thenReturn(parsedAddresses);
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8091"))
					.thenReturn(parsedPolarisAddresses);
			mockedTsf.when(TsfContextUtils::isOnlyTsfConsulEnabled).thenReturn(false);

			// Act
			configurationModifier.modify(configuration);

			// Assert
			ConnectorConfigImpl connectorConfig = configuration.getConfigFile().getServerConnector();
			verify(connectorConfig).setToken("test-token-123");
		}
	}

	/**
	 * Test modify when TsfConsul is the only enabled connector.
	 * Scenario: TsfContextUtils.isOnlyTsfConsulEnabled() returns true.
	 * Expect: reportEnable is set to false on API config.
	 */
	@DisplayName("modify should disable report when only TsfConsul is enabled")
	@Test
	void testModify_TsfConsulEnabled() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn("grpc://127.0.0.1:8093");
		when(polarisConfigProperties.isCheckAddress()).thenReturn(false);
		when(polarisConfigProperties.isEmptyProtectionEnabled()).thenReturn(true);
		when(polarisConfigProperties.getEmptyProtectionExpiredInterval()).thenReturn(604800000L);
		when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);
		when(polarisContextProperties.getAddress()).thenReturn("grpc://127.0.0.1:8091");
		when(polarisContextProperties.getAddressLbPolicy()).thenReturn("roundRobin");
		when(polarisContextProperties.getServerSwitchInterval()).thenReturn(600000L);

		List<String> parsedAddresses = Collections.singletonList("127.0.0.1:8093");
		List<String> parsedPolarisAddresses = Collections.singletonList("127.0.0.1:8091");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class);
				MockedStatic<TsfContextUtils> mockedTsf = Mockito.mockStatic(TsfContextUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8093"))
					.thenReturn(parsedAddresses);
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8091"))
					.thenReturn(parsedPolarisAddresses);
			mockedTsf.when(TsfContextUtils::isOnlyTsfConsulEnabled).thenReturn(true);

			// Act
			configurationModifier.modify(configuration);

			// Assert
			verify(configuration.getGlobal().getAPI()).setReportEnable(false);
		}
	}

	/**
	 * Test modify when polaris context address is blank, fallback to resolve from config address.
	 * Scenario: polarisContextProperties.getAddress() is blank, config address is set.
	 * Expect: report client connector addresses are resolved from config address with port 8091.
	 */
	@DisplayName("modify should resolve polaris address from config address for report client")
	@Test
	void testModify_ReportClientAddressFromConfigAddress() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn("grpc://127.0.0.1:9093");
		when(polarisConfigProperties.isCheckAddress()).thenReturn(false);
		when(polarisConfigProperties.isEmptyProtectionEnabled()).thenReturn(true);
		when(polarisConfigProperties.getEmptyProtectionExpiredInterval()).thenReturn(604800000L);
		when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);
		when(polarisContextProperties.getAddress()).thenReturn("");
		when(polarisContextProperties.getAddressLbPolicy()).thenReturn("roundRobin");
		when(polarisContextProperties.getServerSwitchInterval()).thenReturn(600000L);

		List<String> parsedConfigAddresses = Collections.singletonList("127.0.0.1:9093");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class);
				MockedStatic<TsfContextUtils> mockedTsf = Mockito.mockStatic(TsfContextUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:9093"))
					.thenReturn(parsedConfigAddresses);
			mockedTsf.when(TsfContextUtils::isOnlyTsfConsulEnabled).thenReturn(false);

			// Act
			configurationModifier.modify(configuration);

			// Assert
			ServerConnectorConfigImpl reportConnector = configuration.getGlobal().getServerConnector();
			verify(reportConnector).setAddresses(Collections.singletonList("127.0.0.1:8091"));
		}
	}

	/**
	 * Test modify with checkAddress enabled and accessible address.
	 * Scenario: checkAddress is true, AddressUtils.accessible returns true.
	 * Expect: no exception is thrown.
	 */
	@DisplayName("modify should pass when checkAddress is enabled and address is accessible")
	@Test
	void testModify_CheckAddressAccessible() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn("grpc://127.0.0.1:8093");
		when(polarisConfigProperties.isCheckAddress()).thenReturn(true);
		when(polarisConfigProperties.isEmptyProtectionEnabled()).thenReturn(true);
		when(polarisConfigProperties.getEmptyProtectionExpiredInterval()).thenReturn(604800000L);
		when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);
		when(polarisContextProperties.getAddress()).thenReturn("grpc://127.0.0.1:8091");
		when(polarisContextProperties.getAddressLbPolicy()).thenReturn("roundRobin");
		when(polarisContextProperties.getServerSwitchInterval()).thenReturn(600000L);

		List<String> parsedAddresses = Collections.singletonList("127.0.0.1:8093");
		List<String> parsedPolarisAddresses = Collections.singletonList("127.0.0.1:8091");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class);
				MockedStatic<TsfContextUtils> mockedTsf = Mockito.mockStatic(TsfContextUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8093"))
					.thenReturn(parsedAddresses);
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8091"))
					.thenReturn(parsedPolarisAddresses);
			mockedAddressUtils.when(() -> AddressUtils.accessible("127.0.0.1", 8093, 3000))
					.thenReturn(true);
			mockedTsf.when(TsfContextUtils::isOnlyTsfConsulEnabled).thenReturn(false);

			// Act & Assert - no exception
			configurationModifier.modify(configuration);
		}
	}

	/**
	 * Test modify with checkAddress enabled, address not accessible and shutdown enabled.
	 * Scenario: checkAddress is true, AddressUtils.accessible returns false, shutdownIfConnectToConfigServerFailed is true.
	 * Expect: IllegalArgumentException is thrown.
	 */
	@DisplayName("modify should throw IllegalArgumentException when address not accessible and shutdown enabled")
	@Test
	void testModify_CheckAddressNotAccessibleWithShutdown() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn("grpc://127.0.0.1:8093");
		when(polarisConfigProperties.isCheckAddress()).thenReturn(true);
		when(polarisConfigProperties.isShutdownIfConnectToConfigServerFailed()).thenReturn(true);
		Mockito.lenient().when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);

		List<String> parsedAddresses = Collections.singletonList("127.0.0.1:8093");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class);
				MockedStatic<TsfContextUtils> mockedTsf = Mockito.mockStatic(TsfContextUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8093"))
					.thenReturn(parsedAddresses);
			mockedAddressUtils.when(() -> AddressUtils.accessible("127.0.0.1", 8093, 3000))
					.thenReturn(false);

			// Act & Assert
			assertThatThrownBy(() -> configurationModifier.modify(configuration))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("can not be connected");
		}
	}

	/**
	 * Test modify with checkAddress enabled, address not accessible but shutdown disabled.
	 * Scenario: checkAddress is true, accessible returns false, shutdownIfConnectToConfigServerFailed is false.
	 * Expect: no exception is thrown, error is only logged.
	 */
	@DisplayName("modify should log error when address not accessible and shutdown disabled")
	@Test
	void testModify_CheckAddressNotAccessibleWithoutShutdown() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn("grpc://127.0.0.1:8093");
		when(polarisConfigProperties.isCheckAddress()).thenReturn(true);
		when(polarisConfigProperties.isShutdownIfConnectToConfigServerFailed()).thenReturn(false);
		when(polarisConfigProperties.isEmptyProtectionEnabled()).thenReturn(true);
		when(polarisConfigProperties.getEmptyProtectionExpiredInterval()).thenReturn(604800000L);
		when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);
		when(polarisContextProperties.getAddress()).thenReturn("grpc://127.0.0.1:8091");
		when(polarisContextProperties.getAddressLbPolicy()).thenReturn("roundRobin");
		when(polarisContextProperties.getServerSwitchInterval()).thenReturn(600000L);

		List<String> parsedAddresses = Collections.singletonList("127.0.0.1:8093");
		List<String> parsedPolarisAddresses = Collections.singletonList("127.0.0.1:8091");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class);
				MockedStatic<TsfContextUtils> mockedTsf = Mockito.mockStatic(TsfContextUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8093"))
					.thenReturn(parsedAddresses);
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://127.0.0.1:8091"))
					.thenReturn(parsedPolarisAddresses);
			mockedAddressUtils.when(() -> AddressUtils.accessible("127.0.0.1", 8093, 3000))
					.thenReturn(false);
			mockedTsf.when(TsfContextUtils::isOnlyTsfConsulEnabled).thenReturn(false);

			// Act & Assert - no exception
			configurationModifier.modify(configuration);
		}
	}

	/**
	 * Test checkAddressAccessible with IPv6 address format.
	 * Scenario: address is in IPv6 format like [::1]:8093.
	 * Expect: IPv6 address is parsed correctly.
	 */
	@DisplayName("modify should handle IPv6 address format in checkAddress")
	@Test
	void testModify_CheckAddressWithIpv6() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn("grpc://[::1]:8093");
		when(polarisConfigProperties.isCheckAddress()).thenReturn(true);
		when(polarisConfigProperties.isEmptyProtectionEnabled()).thenReturn(true);
		when(polarisConfigProperties.getEmptyProtectionExpiredInterval()).thenReturn(604800000L);
		when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);
		when(polarisContextProperties.getAddress()).thenReturn("grpc://[::1]:8091");
		when(polarisContextProperties.getAddressLbPolicy()).thenReturn("roundRobin");
		when(polarisContextProperties.getServerSwitchInterval()).thenReturn(600000L);

		List<String> parsedAddresses = Collections.singletonList("[::1]:8093");
		List<String> parsedPolarisAddresses = Collections.singletonList("[::1]:8091");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class);
				MockedStatic<TsfContextUtils> mockedTsf = Mockito.mockStatic(TsfContextUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://[::1]:8093"))
					.thenReturn(parsedAddresses);
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://[::1]:8091"))
					.thenReturn(parsedPolarisAddresses);
			mockedAddressUtils.when(() -> AddressUtils.accessible("::1", 8093, 3000))
					.thenReturn(true);
			mockedTsf.when(TsfContextUtils::isOnlyTsfConsulEnabled).thenReturn(false);

			// Act & Assert - no exception
			configurationModifier.modify(configuration);
		}
	}

	/**
	 * Test checkAddressAccessible with invalid address format (wrong number of parts).
	 * Scenario: address has no port separator resulting in wrong ipPort length.
	 * Expect: IllegalArgumentException is thrown.
	 */
	@DisplayName("modify should throw IllegalArgumentException for invalid address format")
	@Test
	void testModify_CheckAddressInvalidFormat() {
		// Arrange
		ConfigurationImpl configuration = buildMockConfiguration();
		when(polarisContextProperties.getEnabled()).thenReturn(true);
		when(polarisConfigProperties.isEnabled()).thenReturn(true);
		when(polarisConfigProperties.getDataSource()).thenReturn("polaris");
		when(polarisConfigProperties.getAddress()).thenReturn("grpc://invalidaddress");
		when(polarisConfigProperties.isCheckAddress()).thenReturn(true);
		Mockito.lenient().when(polarisCryptoConfigProperties.isEnabled()).thenReturn(false);

		List<String> parsedAddresses = Collections.singletonList("invalidaddress");

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList("grpc://invalidaddress"))
					.thenReturn(parsedAddresses);

			// Act & Assert
			assertThatThrownBy(() -> configurationModifier.modify(configuration))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("is wrong");
		}
	}

	// ==================== resolveConfigAddressFromPolarisAddress tests ====================

	/**
	 * Invoke the private method resolveConfigAddressFromPolarisAddress via reflection.
	 */
	@SuppressWarnings("unchecked")
	private List<String> invokeResolveConfigAddressFromPolarisAddress(String polarisAddress) throws Exception {
		Method method = ConfigurationModifier.class.getDeclaredMethod(
				"resolveConfigAddressFromPolarisAddress", String.class);
		method.setAccessible(true);
		return (List<String>) method.invoke(configurationModifier, polarisAddress);
	}

	/**
	 * Test resolveConfigAddressFromPolarisAddress when polarisAddress is null.
	 * Scenario: pass null as polarisAddress.
	 * Expect: return null.
	 */
	@DisplayName("resolveConfigAddressFromPolarisAddress should return null when polarisAddress is null")
	@Test
	void testResolveConfigAddressFromPolarisAddress_Null() throws Exception {
		// Act
		List<String> result = invokeResolveConfigAddressFromPolarisAddress(null);

		// Assert
		assertThat(result).isNull();
	}

	/**
	 * Test resolveConfigAddressFromPolarisAddress when polarisAddress is empty.
	 * Scenario: pass empty string as polarisAddress.
	 * Expect: return null.
	 */
	@DisplayName("resolveConfigAddressFromPolarisAddress should return null when polarisAddress is empty")
	@Test
	void testResolveConfigAddressFromPolarisAddress_Empty() throws Exception {
		// Act
		List<String> result = invokeResolveConfigAddressFromPolarisAddress("");

		// Assert
		assertThat(result).isNull();
	}

	/**
	 * Test resolveConfigAddressFromPolarisAddress with single address.
	 * Scenario: pass a single polaris address "grpc://127.0.0.1:8091".
	 * Expect: the port should be replaced with config port.
	 */
	@DisplayName("resolveConfigAddressFromPolarisAddress should replace port with config port for single address")
	@Test
	void testResolveConfigAddressFromPolarisAddress_SingleAddress() throws Exception {
		// Arrange
		when(polarisConfigProperties.getPort()).thenReturn(8093);

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Collections.singletonList("127.0.0.1:8091"));

			// Act
			List<String> result = invokeResolveConfigAddressFromPolarisAddress("grpc://127.0.0.1:8091");

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(1);
			assertThat(result.get(0)).isEqualTo("127.0.0.1:8093");
		}
	}

	/**
	 * Test resolveConfigAddressFromPolarisAddress with multiple addresses.
	 * Scenario: pass multiple polaris addresses.
	 * Expect: each port should be replaced with config port.
	 */
	@DisplayName("resolveConfigAddressFromPolarisAddress should replace port for multiple addresses")
	@Test
	void testResolveConfigAddressFromPolarisAddress_MultipleAddresses() throws Exception {
		// Arrange
		when(polarisConfigProperties.getPort()).thenReturn(8093);

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Arrays.asList("127.0.0.1:8091", "127.0.0.1:9091"));

			// Act
			List<String> result = invokeResolveConfigAddressFromPolarisAddress("grpc://127.0.0.1:8091,grpc://127.0.0.1:9091");

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(2);
			assertThat(result.get(0)).isEqualTo("127.0.0.1:8093");
			assertThat(result.get(1)).isEqualTo("127.0.0.1:8093");
		}
	}

	/**
	 * Test resolveConfigAddressFromPolarisAddress with address without port.
	 * Scenario: address has no colon separator.
	 * Expect: address is kept as-is.
	 */
	@DisplayName("resolveConfigAddressFromPolarisAddress should keep address as-is when no port")
	@Test
	void testResolveConfigAddressFromPolarisAddress_AddressWithoutPort() throws Exception {
		// Arrange
		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Collections.singletonList("localhost"));

			// Act
			List<String> result = invokeResolveConfigAddressFromPolarisAddress("grpc://localhost");

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(1);
			assertThat(result.get(0)).isEqualTo("localhost");
		}
	}

	/**
	 * Test resolveConfigAddressFromPolarisAddress with blank address in the list.
	 * Scenario: AddressUtils returns a list containing blank strings.
	 * Expect: blank addresses should be skipped.
	 */
	@DisplayName("resolveConfigAddressFromPolarisAddress should skip blank addresses")
	@Test
	void testResolveConfigAddressFromPolarisAddress_BlankAddressSkipped() throws Exception {
		// Arrange
		when(polarisConfigProperties.getPort()).thenReturn(8093);

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Arrays.asList("127.0.0.1:8091", " ", "127.0.0.1:9091"));

			// Act
			List<String> result = invokeResolveConfigAddressFromPolarisAddress("grpc://127.0.0.1:8091, ,grpc://127.0.0.1:9091");

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(2);
			assertThat(result.get(0)).isEqualTo("127.0.0.1:8093");
			assertThat(result.get(1)).isEqualTo("127.0.0.1:8093");
		}
	}

	// ==================== resolvePolarisAddressFromConfigAddress tests ====================

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
	 * Scenario: pass a single config address "grpc://127.0.0.1:8093".
	 * Expect: the port should be replaced with 8091.
	 */
	@DisplayName("resolvePolarisAddressFromConfigAddress should replace port with 8091 for single address")
	@Test
	void testResolvePolarisAddressFromConfigAddress_SingleAddress() throws Exception {
		// Arrange
		String configAddress = "grpc://127.0.0.1:8093";

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Collections.singletonList("127.0.0.1:8093"));

			// Act
			List<String> result = invokeResolvePolarisAddressFromConfigAddress(configAddress);

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(1);
			assertThat(result.get(0)).isEqualTo("127.0.0.1:8091");
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
		String configAddress = "grpc://127.0.0.1:8093,grpc://127.0.0.1:8094";

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Arrays.asList("127.0.0.1:8093", "127.0.0.1:8094"));

			// Act
			List<String> result = invokeResolvePolarisAddressFromConfigAddress(configAddress);

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(2);
			assertThat(result.get(0)).isEqualTo("127.0.0.1:8091");
			assertThat(result.get(1)).isEqualTo("127.0.0.1:8091");
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
		String configAddress = "grpc://127.0.0.1:8093, ,grpc://127.0.0.1:9090";

		try (MockedStatic<AddressUtils> mockedAddressUtils = Mockito.mockStatic(AddressUtils.class)) {
			mockedAddressUtils.when(() -> AddressUtils.parseAddressList(anyString()))
					.thenReturn(Arrays.asList("127.0.0.1:8093", " ", "127.0.0.1:9090"));

			// Act
			List<String> result = invokeResolvePolarisAddressFromConfigAddress(configAddress);

			// Assert
			assertThat(result).isNotNull();
			assertThat(result).hasSize(2);
			assertThat(result.get(0)).isEqualTo("127.0.0.1:8091");
			assertThat(result.get(1)).isEqualTo("127.0.0.1:8091");
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
