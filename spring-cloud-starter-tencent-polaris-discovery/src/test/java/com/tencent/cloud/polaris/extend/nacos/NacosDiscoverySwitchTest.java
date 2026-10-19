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

package com.tencent.cloud.polaris.extend.nacos;

import java.util.List;

import com.tencent.cloud.polaris.discovery.DiscoveryEnabledCondition;
import com.tencent.cloud.polaris.registry.RegisterEnabledCondition;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test Nacos discovery switch precedence shared by properties, modifier and conditions.
 *
 * @author fishtailfu
 */
public class NacosDiscoverySwitchTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(NacosPropertiesConfiguration.class);

	@Test
	public void testExplicitAgentDisabledDoesNotFallback() {
		this.contextRunner.withPropertyValues(
				"polaris.agent.nacos.discovery.enabled=false",
				"spring.cloud.nacos.discovery.enabled=true",
				"spring.cloud.nacos.discovery.register-enabled=true",
				"spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848",
				"spring.cloud.polaris.discovery.enabled=false",
				"spring.cloud.polaris.discovery.register=false",
				"spring.cloud.consul.enabled=false"
		).run(context -> {
			NacosContextProperties properties = context.getBean(NacosContextProperties.class);
			assertThat(properties.isDiscoveryEnabled()).isFalse();
			assertThat(properties.isRegisterEnabled()).isTrue();
			assertThat(hasNacosConnector(properties)).isFalse();

			ConditionContext conditionContext = conditionContext(context.getEnvironment());
			AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);
			assertThat(new DiscoveryEnabledCondition().matches(conditionContext, metadata)).isTrue();
			assertThat(new RegisterEnabledCondition().matches(conditionContext, metadata)).isTrue();
		});
	}

	@Test
	public void testDiscoveryEnabledWhenAgentPropertyMissing() {
		this.contextRunner.withPropertyValues(
				"spring.cloud.nacos.discovery.enabled=true",
				"spring.cloud.nacos.discovery.register-enabled=false",
				"spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848",
				"spring.cloud.nacos.discovery.service=order-service",
				"spring.cloud.polaris.discovery.enabled=false",
				"spring.cloud.polaris.discovery.register=false",
				"spring.cloud.consul.enabled=false"
		).run(context -> {
			NacosContextProperties properties = context.getBean(NacosContextProperties.class);
			assertThat(properties.isDiscoveryEnabled()).isTrue();
			assertThat(properties.isRegisterEnabled()).isFalse();
			assertThat(hasNacosConnector(properties)).isTrue();

			ConditionContext conditionContext = conditionContext(context.getEnvironment());
			AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);
			assertThat(new DiscoveryEnabledCondition().matches(conditionContext, metadata)).isTrue();
			assertThat(new RegisterEnabledCondition().matches(conditionContext, metadata)).isFalse();
		});
	}

	private boolean hasNacosConnector(NacosContextProperties properties) {
		ConfigurationImpl configuration = new ConfigurationImpl();
		configuration.setDefault();
		new NacosConfigModifier(properties).modify(configuration);
		List<ServerConnectorConfigImpl> connectors = configuration.getGlobal().getServerConnectors();
		if (connectors == null) {
			return false;
		}
		return connectors.stream().anyMatch(connector -> "nacos".equals(connector.getId()));
	}

	private ConditionContext conditionContext(Environment environment) {
		ConditionContext conditionContext = Mockito.mock(ConditionContext.class);
		when(conditionContext.getEnvironment()).thenReturn(environment);
		return conditionContext;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(NacosContextProperties.class)
	static class NacosPropertiesConfiguration {

	}
}
