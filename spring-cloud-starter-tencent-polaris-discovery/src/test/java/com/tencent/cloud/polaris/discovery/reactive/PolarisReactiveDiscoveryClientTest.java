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
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.cloud.client.ServiceInstance;

import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisReactiveDiscoveryClient}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
public class PolarisReactiveDiscoveryClientTest {

	@Mock
	private PolarisServiceDiscovery serviceDiscovery;

	@InjectMocks
	private PolarisReactiveDiscoveryClient client;

	private int count = 0;

	@Test
	public void testGetInstances() throws PolarisException {

		when(serviceDiscovery.getInstances(anyString())).thenAnswer(invocation -> {
			String serviceName = invocation.getArgument(0);
			if (SERVICE_PROVIDER.equalsIgnoreCase(serviceName)) {
				return singletonList(mock(ServiceInstance.class));
			}
			else {
				throw new PolarisException(ErrorCode.UNKNOWN_SERVER_ERROR);
			}
		});

		// Normal
		Flux<ServiceInstance> instances = this.client.getInstances(SERVICE_PROVIDER);
		StepVerifier.create(instances).expectNextCount(1).expectComplete().verify();

		// PolarisException
		instances = this.client.getInstances(SERVICE_PROVIDER + 1);
		StepVerifier.create(instances).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void testGetServices() throws PolarisException {

		when(serviceDiscovery.getServices()).thenAnswer(invocation -> {
			if (count == 0) {
				count++;
				return Arrays.asList(SERVICE_PROVIDER + 1, SERVICE_PROVIDER + 2);
			}
			else {
				throw new PolarisException(ErrorCode.UNKNOWN_SERVER_ERROR);
			}
		});

		// Normal
		Flux<String> services = this.client.getServices();
		StepVerifier.create(services).expectNext(SERVICE_PROVIDER + 1, SERVICE_PROVIDER + 2).expectComplete().verify();

		// PolarisException
		services = this.client.getServices();
		StepVerifier.create(services).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void testDescription() {
		assertThat(client.description()).isEqualTo("Spring Cloud Tencent Polaris Reactive Discovery Client");
	}
}
