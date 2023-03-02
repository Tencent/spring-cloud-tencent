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

package com.tencent.cloud.polaris.discovery;

import java.util.List;

import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServicesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;

import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisServiceDiscovery}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
public class PolarisServiceDiscoveryTest {

	@Mock
	private PolarisDiscoveryHandler polarisDiscoveryHandler;
	@InjectMocks
	private PolarisServiceDiscovery polarisServiceDiscovery;

	@BeforeEach
	void setUp() {
		new ApplicationContextAwareUtils().setApplicationContext(new StaticApplicationContext());
	}

	@Test
	public void testGetInstances() {
		DefaultServiceInstances mockDefaultServiceInstances = mock(DefaultServiceInstances.class);
		when(mockDefaultServiceInstances.getInstances()).thenReturn(singletonList(mock(DefaultInstance.class)));
		InstancesResponse mockInstancesResponse = mock(InstancesResponse.class);
		when(mockInstancesResponse.toServiceInstances()).thenReturn(mockDefaultServiceInstances);
		when(polarisDiscoveryHandler.getHealthyInstances(anyString())).thenReturn(mockInstancesResponse);

		List<ServiceInstance> serviceInstances = polarisServiceDiscovery.getInstances(SERVICE_PROVIDER);

		assertThat(serviceInstances).isNotEmpty();
	}

	@Test
	public void testGetServices() throws PolarisException {
		ServiceInfo mockServiceInfo = mock(ServiceInfo.class);
		when(mockServiceInfo.getService()).thenReturn(SERVICE_PROVIDER);
		ServicesResponse mockServicesResponse = mock(ServicesResponse.class);
		when(mockServicesResponse.getServices()).thenReturn(singletonList(mockServiceInfo));
		when(polarisDiscoveryHandler.getServices()).thenReturn(mockServicesResponse);

		List<String> services = polarisServiceDiscovery.getServices();

		assertThat(services).size().isEqualTo(1);
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisPropertiesConfiguration {

	}
}
