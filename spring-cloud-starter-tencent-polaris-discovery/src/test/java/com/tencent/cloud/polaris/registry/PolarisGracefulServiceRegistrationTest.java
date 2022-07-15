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
import com.tencent.cloud.polaris.registry.graceful.GracefulServiceRegistrationProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

/**
 * Test for {@link PolarisAutoServiceRegistration}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisGracefulServiceRegistrationTest {

	@Mock
	private ServiceRegistry<Registration> serviceRegistry;

	@Mock
	private GracefulServiceRegistrationProperties autoServiceRegistrationProperties;

	@Mock
	private PolarisDiscoveryProperties polarisDiscoveryProperties;

	@Mock
	private ApplicationContext applicationContext;

	@Mock
	private Environment environment;

	@Mock
	private PolarisRegistration registration;

	private PolarisAutoServiceRegistration polarisAutoServiceRegistration;

	@Before
	public void setUp() {
		doReturn(polarisDiscoveryProperties).when(registration).getPolarisProperties();

		doNothing().when(serviceRegistry).register(nullable(Registration.class));

		polarisAutoServiceRegistration =
				new PolarisAutoServiceRegistration(serviceRegistry, autoServiceRegistrationProperties, registration);

		doReturn(environment).when(applicationContext).getEnvironment();
		polarisAutoServiceRegistration.setApplicationContext(applicationContext);
	}

	@Test
	public void testRegister() {
		doReturn(false).when(registration).isRegisterEnabled();
		try {
			polarisAutoServiceRegistration.register();
		}
		catch (Exception e) {
			fail();
		}

		doReturn(true).when(registration).isRegisterEnabled();
		doReturn(-1).when(registration).getPort();
		try {
			polarisAutoServiceRegistration.register();
		}
		catch (Exception e) {
			fail();
		}

		doReturn(PORT).when(registration).getPort();
		try {
			polarisAutoServiceRegistration.register();
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	public void testGetManagementRegistration() {
		assertThat(polarisAutoServiceRegistration.getManagementRegistration()).isNull();
	}

	@Test
	public void testRegisterManagement() {
		doReturn(false).when(registration).isRegisterEnabled();
		try {
			polarisAutoServiceRegistration.registerManagement();
		}
		catch (Exception e) {
			fail();
		}

		doReturn(true).when(registration).isRegisterEnabled();
		try {
			polarisAutoServiceRegistration.registerManagement();
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	public void testGetAppName() {
		doReturn("application").when(environment).getProperty(anyString(), anyString());
		assertThat(polarisAutoServiceRegistration.getAppName()).isEqualTo("application");

		doReturn(SERVICE_PROVIDER).when(polarisDiscoveryProperties).getService();
		assertThat(polarisAutoServiceRegistration.getAppName()).isEqualTo(SERVICE_PROVIDER);
	}
}
