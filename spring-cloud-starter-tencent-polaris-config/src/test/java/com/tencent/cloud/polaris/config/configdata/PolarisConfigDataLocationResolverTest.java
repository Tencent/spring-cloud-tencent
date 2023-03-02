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
 *
 */

package com.tencent.cloud.polaris.config.configdata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogs;
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
}
