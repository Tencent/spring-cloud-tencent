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

package com.tencent.cloud.polaris.discovery;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DiscoveryEnabledCondition}.
 *
 * @author fishtailfu
 */
public class DiscoveryEnabledConditionTest {

	private final DiscoveryEnabledCondition condition = new DiscoveryEnabledCondition();

	@Test
	public void testPolarisDiscoveryEnabled() {
		ConditionContext context = Mockito.mock(ConditionContext.class);
		Environment environment = Mockito.mock(Environment.class);
		AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);

		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.polaris.discovery.enabled", "true")).thenReturn("true");
		when(environment.getProperty("spring.cloud.consul.enabled", "false")).thenReturn("false");
		when(environment.getProperty("spring.cloud.nacos.discovery.enabled", "false")).thenReturn("false");
		when(environment.getProperty("polaris.agent.nacos.discovery.enabled", "false")).thenReturn("false");

		boolean result = condition.matches(context, metadata);
		assertThat(result).isTrue();
	}

	@Test
	public void testPolarisDiscoveryDisabled() {
		ConditionContext context = Mockito.mock(ConditionContext.class);
		Environment environment = Mockito.mock(Environment.class);
		AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);

		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.polaris.discovery.enabled", "true")).thenReturn("false");
		when(environment.getProperty("spring.cloud.consul.enabled", "false")).thenReturn("false");
		when(environment.getProperty("spring.cloud.nacos.discovery.enabled", "false")).thenReturn("false");
		when(environment.getProperty("polaris.agent.nacos.discovery.enabled", "false")).thenReturn("false");

		boolean result = condition.matches(context, metadata);
		assertThat(result).isFalse();
	}

	@Test
	public void testConsulDiscoveryEnabled() {
		ConditionContext context = Mockito.mock(ConditionContext.class);
		Environment environment = Mockito.mock(Environment.class);
		AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);

		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.polaris.discovery.enabled", "true")).thenReturn("false");
		when(environment.getProperty("spring.cloud.consul.enabled", "false")).thenReturn("true");
		when(environment.getProperty("spring.cloud.consul.discovery.enabled", "true")).thenReturn("true");
		when(environment.getProperty("spring.cloud.nacos.discovery.enabled", "false")).thenReturn("false");
		when(environment.getProperty("polaris.agent.nacos.discovery.enabled", "false")).thenReturn("false");

		boolean result = condition.matches(context, metadata);
		assertThat(result).isTrue();
	}

	@Test
	public void testNacosDiscoveryEnabled() {
		ConditionContext context = Mockito.mock(ConditionContext.class);
		Environment environment = Mockito.mock(Environment.class);
		AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);

		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.polaris.discovery.enabled", "true")).thenReturn("false");
		when(environment.getProperty("spring.cloud.consul.enabled", "false")).thenReturn("false");
		when(environment.getProperty("spring.cloud.nacos.discovery.enabled", "false")).thenReturn("true");
		when(environment.getProperty("polaris.agent.nacos.discovery.enabled", "false")).thenReturn("false");

		boolean result = condition.matches(context, metadata);
		assertThat(result).isTrue();
	}

	@Test
	public void testPolarisAgentNacosDiscoveryEnabled() {
		ConditionContext context = Mockito.mock(ConditionContext.class);
		Environment environment = Mockito.mock(Environment.class);
		AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);

		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.polaris.discovery.enabled", "true")).thenReturn("false");
		when(environment.getProperty("spring.cloud.consul.enabled", "false")).thenReturn("false");
		when(environment.getProperty("spring.cloud.nacos.discovery.enabled", "false")).thenReturn("false");
		when(environment.getProperty("polaris.agent.nacos.discovery.enabled", "false")).thenReturn("true");

		boolean result = condition.matches(context, metadata);
		assertThat(result).isTrue();
	}

	@Test
	public void testBothNacosPropertiesEnabled() {
		ConditionContext context = Mockito.mock(ConditionContext.class);
		Environment environment = Mockito.mock(Environment.class);
		AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);

		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.polaris.discovery.enabled", "true")).thenReturn("false");
		when(environment.getProperty("spring.cloud.consul.enabled", "false")).thenReturn("false");
		when(environment.getProperty("spring.cloud.nacos.discovery.enabled", "false")).thenReturn("true");
		when(environment.getProperty("polaris.agent.nacos.discovery.enabled", "false")).thenReturn("true");

		boolean result = condition.matches(context, metadata);
		assertThat(result).isTrue();
	}

	@Test
	public void testAllDiscoveryDisabled() {
		ConditionContext context = Mockito.mock(ConditionContext.class);
		Environment environment = Mockito.mock(Environment.class);
		AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);

		when(context.getEnvironment()).thenReturn(environment);
		when(environment.getProperty("spring.cloud.polaris.discovery.enabled", "true")).thenReturn("false");
		when(environment.getProperty("spring.cloud.consul.enabled", "false")).thenReturn("false");
		when(environment.getProperty("spring.cloud.consul.discovery.enabled", "true")).thenReturn("false");
		when(environment.getProperty("spring.cloud.nacos.discovery.enabled", "false")).thenReturn("false");
		when(environment.getProperty("polaris.agent.nacos.discovery.enabled", "false")).thenReturn("false");

		boolean result = condition.matches(context, metadata);
		assertThat(result).isFalse();
	}
}
