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

package com.tencent.cloud.polaris.context.listener;

import java.security.Permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.Mockito.when;


class FailedEventApplicationListenerTest {

	@Mock
	private ConfigurableApplicationContext mockConfigurableContext;

	@Mock
	private ApplicationContext mockApplicationContext;

	@Mock
	private ApplicationFailedEvent mockFailedEvent;

	private FailedEventApplicationListener listener;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		listener = new FailedEventApplicationListener();
	}

	@Test
	void testSetApplicationContext() {
		// Test setting application context
		listener.setApplicationContext(mockApplicationContext);
	}

	@Test
	void testOnApplicationEventWithConfigurableContext() {
		// Arrange
		listener.setApplicationContext(mockConfigurableContext);
		when(mockFailedEvent.getException()).thenReturn(new RuntimeException("Test Exception"));

		SecurityManager originalSecurityManager = System.getSecurityManager();
		System.setSecurityManager(new ExitSecurityManager());

		try {
			// Act
			listener.onApplicationEvent(mockFailedEvent);
		}
		catch (SecurityException e) {
			// Ignore
		}
		finally {
			System.setSecurityManager(originalSecurityManager);
		}
	}

	@Test
	void testOnApplicationEventWithNonConfigurableContext() {
		// Arrange
		listener.setApplicationContext(mockApplicationContext);
		when(mockFailedEvent.getException()).thenReturn(new RuntimeException("Test Exception"));

		SecurityManager originalSecurityManager = System.getSecurityManager();
		System.setSecurityManager(new ExitSecurityManager());

		try {
			// Act
			listener.onApplicationEvent(mockFailedEvent);
		}
		catch (SecurityException e) {
			// Ignore
		}
		finally {
			System.setSecurityManager(originalSecurityManager);
		}
	}

	@Test
	void testOnApplicationEventWithInterruptedException() {
		// Arrange
		listener.setApplicationContext(mockConfigurableContext);
		Thread.currentThread().interrupt(); // Simulate interruption


		SecurityManager originalSecurityManager = System.getSecurityManager();
		System.setSecurityManager(new ExitSecurityManager());

		try {
			// Act
			listener.onApplicationEvent(mockFailedEvent);
		}
		catch (SecurityException e) {
			// Ignore
		}
		finally {
			System.setSecurityManager(originalSecurityManager);
		}
	}

	@Test
	void testOnApplicationEventWithNullException() {
		// Arrange
		listener.setApplicationContext(mockConfigurableContext);
		when(mockFailedEvent.getException()).thenReturn(null);


		SecurityManager originalSecurityManager = System.getSecurityManager();
		System.setSecurityManager(new ExitSecurityManager());

		try {
			// Act
			listener.onApplicationEvent(mockFailedEvent);
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
