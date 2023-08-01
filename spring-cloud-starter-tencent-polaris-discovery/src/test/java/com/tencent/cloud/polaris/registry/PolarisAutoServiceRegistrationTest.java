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
 */

package com.tencent.cloud.polaris.registry;

import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

/**
 * Test for {@link PolarisAutoServiceRegistration}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PolarisAutoServiceRegistrationTest {

	@Mock
	private ServiceRegistry<PolarisRegistration> serviceRegistry;

	@Mock
	private AutoServiceRegistrationProperties autoServiceRegistrationProperties;

	@Mock
	private PolarisDiscoveryProperties polarisDiscoveryProperties;

	@Mock
	private ApplicationContext applicationContext;

	@Mock
	private Environment environment;

	@Mock
	private PolarisRegistration registration;

	private PolarisAutoServiceRegistration polarisAutoServiceRegistration;

	@BeforeEach
	void setUp() {
		doNothing().when(serviceRegistry).register(nullable(PolarisRegistration.class));
		doNothing().when(serviceRegistry).deregister(nullable(PolarisRegistration.class));

		polarisAutoServiceRegistration =
				new PolarisAutoServiceRegistration(serviceRegistry, autoServiceRegistrationProperties, registration,
						polarisDiscoveryProperties, null);

		doReturn(environment).when(applicationContext).getEnvironment();
		polarisAutoServiceRegistration.setApplicationContext(applicationContext);
	}

	@Test
	public void testRegister() {
		doReturn(false).when(registration).isRegisterEnabled();
		assertThatCode(() -> {
			polarisAutoServiceRegistration.register();
		}).doesNotThrowAnyException();

		doReturn(true).when(registration).isRegisterEnabled();
		assertThatCode(() -> {
			polarisAutoServiceRegistration.register();
		}).doesNotThrowAnyException();
	}

	@Test
	public void testGetManagementRegistration() {
		assertThat(polarisAutoServiceRegistration.getManagementRegistration()).isNull();
	}

	@Test
	public void testRegisterManagement() {
		doReturn(false).when(registration).isRegisterEnabled();
		assertThatCode(() -> {
			polarisAutoServiceRegistration.registerManagement();
		}).doesNotThrowAnyException();

		doReturn(true).when(registration).isRegisterEnabled();
		assertThatCode(() -> {
			polarisAutoServiceRegistration.registerManagement();
		}).doesNotThrowAnyException();
	}

	@Test
	public void testDeregister() {
		doReturn(false).when(registration).isRegisterEnabled();
		assertThatCode(() -> {
			polarisAutoServiceRegistration.registerManagement();
		}).doesNotThrowAnyException();

		doReturn(true).when(registration).isRegisterEnabled();
		assertThatCode(() -> {
			polarisAutoServiceRegistration.deregister();
		}).doesNotThrowAnyException();
	}

	@Test
	public void testDeregisterManagement() {
		doReturn(false).when(registration).isRegisterEnabled();
		assertThatCode(() -> {
			polarisAutoServiceRegistration.registerManagement();
		}).doesNotThrowAnyException();

		doReturn(true).when(registration).isRegisterEnabled();
		assertThatCode(() -> {
			polarisAutoServiceRegistration.deregisterManagement();
		}).doesNotThrowAnyException();
	}

	@Test
	public void testGetAppName() {
		doReturn("application").when(environment).getProperty(anyString(), anyString());
		assertThat(polarisAutoServiceRegistration.getAppName()).isEqualTo("application");

		doReturn(SERVICE_PROVIDER).when(registration).getServiceId();
		assertThat(polarisAutoServiceRegistration.getAppName()).isEqualTo(SERVICE_PROVIDER);
	}
}
