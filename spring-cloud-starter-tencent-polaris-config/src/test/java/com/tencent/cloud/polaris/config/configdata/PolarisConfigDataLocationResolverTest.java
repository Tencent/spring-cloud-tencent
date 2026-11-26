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

package com.tencent.cloud.polaris.config.configdata;

import java.util.Collections;
import java.util.List;

import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.config.PolarisCryptoConfigProperties;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.context.event.PushGatewayEventReporterConfigModifier;
import com.tencent.polaris.api.config.ConfigProvider;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.event.pushgateway.PushGatewayEventReporterConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisConfigDataLocationResolver}.
 *
 * @author wlx
 */
@ExtendWith(MockitoExtension.class)
public class PolarisConfigDataLocationResolverTest {

	private final PolarisConfigDataLocationResolver resolver = new PolarisConfigDataLocationResolver(new DeferredLogs());
	private final MockEnvironment environment = new MockEnvironment();
	private final Binder environmentBinder = Binder.get(this.environment);
	@Mock
	private ConfigDataLocationResolverContext context;
	@Mock
	private ConfigurableBootstrapContext bootstrapContext;

	@Test
	public void testIsResolvable() {
		when(context.getBinder()).thenReturn(environmentBinder);
		assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.of("configserver:"))).isFalse();
		assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.of("polaris:"))).isTrue();
		assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.of("polaris"))).isTrue();
	}

	@Test
	public void unEnabledPolarisConfigData() {
		environment.setProperty("spring.cloud.polaris.config.enabled", "false");
		when(context.getBinder()).thenReturn(environmentBinder);
		assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.of("polaris:"))).isFalse();
	}

	/**
	 * The config data temp SDK context reports the config event of the initial load, so it applies
	 * the push gateway properties.
	 */
	@Test
	public void testModifierListApplyPushGateway() {
		// Arrange
		environment.setProperty("spring.cloud.polaris.event.pushgateway.enabled", "true");
		environment.setProperty("spring.cloud.polaris.event.pushgateway.address", "127.0.0.1:9091");
		when(context.getBinder()).thenReturn(environmentBinder);
		when(context.getBootstrapContext()).thenReturn(bootstrapContext);
		ConfigurationImpl configuration = (ConfigurationImpl) ConfigAPIFactory.defaultConfig(ConfigProvider.DEFAULT_CONFIG);

		// Act
		applyPushGatewayModifier(configuration);

		// Assert
		assertThat(configuration.getGlobal().getEventReporter().getReporters())
				.contains(DefaultPlugins.PUSH_GATEWAY_EVENT_REPORTER_TYPE);
		PushGatewayEventReporterConfig config = configuration.getGlobal().getEventReporter()
				.getPluginConfig(DefaultPlugins.PUSH_GATEWAY_EVENT_REPORTER_TYPE, PushGatewayEventReporterConfig.class);
		assertThat(config.isEnable()).isTrue();
		assertThat(config.getAddress()).containsExactly("127.0.0.1:9091");
	}

	/**
	 * Push gateway stays disabled in the config data temp SDK context unless it is enabled.
	 */
	@Test
	public void testModifierListPushGatewayDisabledByDefault() {
		// Arrange
		when(context.getBinder()).thenReturn(environmentBinder);
		when(context.getBootstrapContext()).thenReturn(bootstrapContext);
		ConfigurationImpl configuration = (ConfigurationImpl) ConfigAPIFactory.defaultConfig(ConfigProvider.DEFAULT_CONFIG);

		// Act
		applyPushGatewayModifier(configuration);

		// Assert
		PushGatewayEventReporterConfig config = configuration.getGlobal().getEventReporter()
				.getPluginConfig(DefaultPlugins.PUSH_GATEWAY_EVENT_REPORTER_TYPE, PushGatewayEventReporterConfig.class);
		assertThat(config.isEnable()).isFalse();
	}

	/**
	 * spring.cloud.client.ip-address is used as is when it is set.
	 */
	@Test
	public void testResolveIpAddressFromProperty() {
		// Arrange
		environment.setProperty("spring.cloud.client.ip-address", "127.0.0.1");
		when(context.getBinder()).thenReturn(environmentBinder);
		when(context.getBootstrapContext()).thenReturn(bootstrapContext);

		// Act
		String ipAddress = resolver.resolveIpAddress(context);

		// Assert
		assertThat(ipAddress).isEqualTo("127.0.0.1");
	}

	/**
	 * Without spring.cloud.client.ip-address, the ip is resolved by InetUtils with the
	 * spring.cloud.inetutils properties, the same as the ip of the registered instance.
	 */
	@Test
	public void testResolveIpAddressByInetUtils() {
		// Arrange
		environment.setProperty("spring.cloud.inetutils.preferred-networks", "127.0.0.1");
		when(context.getBinder()).thenReturn(environmentBinder);
		when(context.getBootstrapContext()).thenReturn(bootstrapContext);
		InetUtilsProperties inetUtilsProperties = new InetUtilsProperties();
		inetUtilsProperties.setPreferredNetworks(Collections.singletonList("127.0.0.1"));
		String expected;
		try (InetUtils inetUtils = new InetUtils(inetUtilsProperties)) {
			expected = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
		}

		// Act
		String ipAddress = resolver.resolveIpAddress(context);

		// Assert
		assertThat(ipAddress).isNotBlank().isEqualTo(expected);
	}

	private void applyPushGatewayModifier(ConfigurationImpl configuration) {
		List<PolarisConfigModifier> modifierList = resolver.modifierList(context, new PolarisConfigProperties(),
				new PolarisCryptoConfigProperties(), new PolarisContextProperties());
		modifierList.stream()
				.filter(modifier -> modifier instanceof PushGatewayEventReporterConfigModifier)
				.forEach(modifier -> modifier.modify(configuration));
		assertThat(modifierList).hasAtLeastOneElementOfType(PushGatewayEventReporterConfigModifier.class);
	}
}
