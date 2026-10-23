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

package com.tencent.cloud.polaris.eager.instrument.services;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ServicesEagerLoadSmartLifecycle}.
 *
 * @author fishtailfu
 */
public class ServicesEagerLoadSmartLifecycleTest {

	@Test
	public void testLoadServicesFromBlockingClient() {
		PolarisDiscoveryClient discoveryClient = mock(PolarisDiscoveryClient.class);
		PolarisReactiveDiscoveryClient reactiveDiscoveryClient = mock(PolarisReactiveDiscoveryClient.class);
		when(discoveryClient.getServices()).thenReturn(Arrays.asList("order", "account"));

		ServicesEagerLoadSmartLifecycle lifecycle = lifecycle(discoveryClient, reactiveDiscoveryClient);
		lifecycle.start();

		verify(discoveryClient).getServices();
		verifyNoInteractions(reactiveDiscoveryClient);
		assertThatCode(lifecycle::stop).doesNotThrowAnyException();
		assertThat(lifecycle.isRunning()).isFalse();
		assertThat(lifecycle.getPhase()).isEqualTo(10);
	}

	@Test
	public void testLoadServicesFromReactiveClient() {
		PolarisReactiveDiscoveryClient reactiveDiscoveryClient = mock(PolarisReactiveDiscoveryClient.class);
		AtomicBoolean subscribed = new AtomicBoolean(false);
		when(reactiveDiscoveryClient.getServices()).thenReturn(Flux.just("order")
				.doOnSubscribe(subscription -> subscribed.set(true)));

		lifecycle(null, reactiveDiscoveryClient).start();

		assertThat(subscribed).isTrue();
	}

	@Test
	public void testNoDiscoveryClient() {
		assertThatCode(() -> lifecycle(null, null).start()).doesNotThrowAnyException();
	}

	@Test
	public void testBlockingClientFailureIsIgnored() {
		PolarisDiscoveryClient discoveryClient = mock(PolarisDiscoveryClient.class);
		when(discoveryClient.getServices()).thenThrow(new IllegalStateException("discover failed"));

		assertThatCode(() -> lifecycle(discoveryClient, null).start()).doesNotThrowAnyException();
	}

	@Test
	public void testReactiveClientFailureIsIgnored() {
		PolarisReactiveDiscoveryClient reactiveDiscoveryClient = mock(PolarisReactiveDiscoveryClient.class);
		when(reactiveDiscoveryClient.getServices()).thenReturn(Flux.error(new IllegalStateException("discover failed")));

		assertThatCode(() -> lifecycle(null, reactiveDiscoveryClient).start()).doesNotThrowAnyException();
	}

	private ServicesEagerLoadSmartLifecycle lifecycle(PolarisDiscoveryClient discoveryClient,
			PolarisReactiveDiscoveryClient reactiveDiscoveryClient) {
		return new ServicesEagerLoadSmartLifecycle(discoveryClient, reactiveDiscoveryClient);
	}
}
