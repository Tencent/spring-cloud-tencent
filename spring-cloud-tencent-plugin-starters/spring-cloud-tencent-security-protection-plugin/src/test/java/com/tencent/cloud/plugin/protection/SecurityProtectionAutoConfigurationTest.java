/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.plugin.protection;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.servlet.function.RouterFunction;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link SecurityProtectionAutoConfiguration}.
 */
@ExtendWith(MockitoExtension.class)
class SecurityProtectionAutoConfigurationTest {

	@Mock
	private ConfigurableApplicationContext applicationContext;

	@Test
	void testServletProtectionNoRouterFunctions() {
		// Arrange
		SecurityProtectionAutoConfiguration.ServletProtectionConfiguration config =
				new SecurityProtectionAutoConfiguration.ServletProtectionConfiguration();
		config.applicationContext = applicationContext;
		config.routerFunctions = null;

		// Act
		config.afterPropertiesSet();

		// Verify
		// Should not call exit when no RouterFunctions present
		verify(applicationContext, never()).close();
	}

	@Test
	void testServletProtectionEmptyRouterFunctions() {
		// Arrange
		SecurityProtectionAutoConfiguration.ServletProtectionConfiguration config =
				new SecurityProtectionAutoConfiguration.ServletProtectionConfiguration();
		config.applicationContext = applicationContext;
		config.routerFunctions = new ArrayList<>();

		// Act
		config.afterPropertiesSet();

		// Verify
		// Should not call exit when RouterFunctions list is empty
		verify(applicationContext, never()).close();
	}

	@Test
	void testServletProtectionWithRouterFunctions() {
		// Arrange
		SecurityProtectionAutoConfiguration.ServletProtectionConfiguration config =
				new SecurityProtectionAutoConfiguration.ServletProtectionConfiguration();
		config.applicationContext = mock(ConfigurableApplicationContext.class);
		List<RouterFunction> routerFunctions = new ArrayList<>();
		routerFunctions.add(mock(RouterFunction.class));
		config.routerFunctions = routerFunctions;

		SecurityManager originalSecurityManager = System.getSecurityManager();
		System.setSecurityManager(new ExitSecurityManager());

		try {
			config.afterPropertiesSet();
		}
		catch (SecurityException e) {
			// Ignore
		}
		finally {
			System.setSecurityManager(originalSecurityManager);
		}
	}

	@Test
	void testReactiveProtectionNoRouterFunctions() {
		// Arrange
		SecurityProtectionAutoConfiguration.ReactiveProtectionConfiguration config =
				new SecurityProtectionAutoConfiguration.ReactiveProtectionConfiguration();
		config.applicationContext = applicationContext;
		config.routerFunctions = null;

		// Act
		config.afterPropertiesSet();

		// Verify
		verify(applicationContext, never()).close();
	}

	@Test
	void testReactiveProtectionEmptyRouterFunctions() {
		// Arrange
		SecurityProtectionAutoConfiguration.ReactiveProtectionConfiguration config =
				new SecurityProtectionAutoConfiguration.ReactiveProtectionConfiguration();
		config.applicationContext = applicationContext;
		config.routerFunctions = new ArrayList<>();

		// Act
		config.afterPropertiesSet();

		// Verify
		verify(applicationContext, never()).close();
	}

	@Test
	void testReactiveProtectionWithRouterFunctions() {
		// Arrange
		SecurityProtectionAutoConfiguration.ReactiveProtectionConfiguration config =
				new SecurityProtectionAutoConfiguration.ReactiveProtectionConfiguration();
		config.applicationContext = mock(ConfigurableApplicationContext.class);
		List<org.springframework.web.reactive.function.server.RouterFunction> routerFunctions = new ArrayList<>();
		routerFunctions.add(mock(org.springframework.web.reactive.function.server.RouterFunction.class));
		config.routerFunctions = routerFunctions;

		SecurityManager originalSecurityManager = System.getSecurityManager();
		System.setSecurityManager(new ExitSecurityManager());

		try {
			config.afterPropertiesSet();
		}
		catch (SecurityException e) {
			// Ignore
		}
		finally {
			System.setSecurityManager(originalSecurityManager);
		}
	}

	@Test
	void testInterruptedExceptionHandling() throws InterruptedException {
		// Arrange
		ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);
		Thread testThread = new Thread(() -> ExitUtils.exit(mockContext, 3000));



		SecurityManager originalSecurityManager = System.getSecurityManager();
		System.setSecurityManager(new ExitSecurityManager());

		try {
			// Act
			testThread.start();
			testThread.interrupt();
			Thread.sleep(6000);
		}
		catch (SecurityException e) {
			// Ignore
		}
		finally {
			System.setSecurityManager(originalSecurityManager);
		}
	}

	public static class ExitSecurityManager extends SecurityManager {

		@Override
		public void checkPermission(Permission perm) {
			if (perm.getName().contains("exitVM")) {
				throw new SecurityException("System.exit is not allowed");
			}
		}
	}
}
