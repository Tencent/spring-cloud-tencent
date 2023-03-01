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

package com.tencent.cloud.polaris.circuitbreaker.reactor;

import java.util.function.Function;

import com.tencent.polaris.circuitbreak.api.InvokeHandler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactor Transformer for PolarisCircuitBreaker.
 *
 * @author seanyu 2023-02-27
 */
public class PolarisCircuitBreakerReactorTransformer<T> implements Function<Publisher<T>, Publisher<T>> {

	private final InvokeHandler invokeHandler;

	public PolarisCircuitBreakerReactorTransformer(InvokeHandler invokeHandler) {
		this.invokeHandler = invokeHandler;
	}

	@Override
	public Publisher<T> apply(Publisher<T> publisher) {
		if (publisher instanceof Mono) {
			return new PolarisCircuitBreakerMonoOperator<>((Mono<? extends T>) publisher, invokeHandler);
		}
		else if (publisher instanceof Flux) {
			return new PolarisCircuitBreakerFluxOperator<>((Flux<? extends T>) publisher, invokeHandler);
		}
		else {
			throw new IllegalStateException("Publisher type is not supported: " + publisher.getClass().getCanonicalName());
		}
	}

}
