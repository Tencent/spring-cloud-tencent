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

package com.tencent.cloud.polaris.discovery.reactive;

import java.util.Arrays;

import com.tencent.cloud.polaris.discovery.PolarisServiceDiscovery;
import com.tencent.polaris.api.exception.PolarisException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.cloud.client.ServiceInstance;

import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisReactiveDiscoveryClient}
 *
 * @author Haotian Zhang
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class PolarisReactiveDiscoveryClientTest {

	@Mock
	private PolarisServiceDiscovery serviceDiscovery;

	@Mock
	private ServiceInstance serviceInstance;

	@InjectMocks
	private PolarisReactiveDiscoveryClient client;

	@Test
	public void testGetInstances() throws PolarisException {

		when(serviceDiscovery.getInstances(SERVICE_PROVIDER))
				.thenReturn(singletonList(serviceInstance));

		Flux<ServiceInstance> instances = this.client.getInstances(SERVICE_PROVIDER);

		StepVerifier.create(instances).expectNextCount(1).expectComplete().verify();
	}

	@Test
	public void testGetServices() throws PolarisException {

		when(serviceDiscovery.getServices())
				.thenReturn(Arrays.asList(SERVICE_PROVIDER + 1, SERVICE_PROVIDER + 2));

		Flux<String> services = this.client.getServices();

		StepVerifier.create(services)
				.expectNext(SERVICE_PROVIDER + 1, SERVICE_PROVIDER + 2).expectComplete()
				.verify();
	}

}
