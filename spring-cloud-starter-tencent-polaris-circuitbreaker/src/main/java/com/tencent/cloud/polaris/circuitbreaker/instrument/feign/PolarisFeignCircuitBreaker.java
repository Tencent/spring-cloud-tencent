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

package com.tencent.cloud.polaris.circuitbreaker.instrument.feign;

import feign.Feign;
import feign.Target;

import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * PolarisFeignCircuitBreaker, mostly copy from {@link org.springframework.cloud.openfeign.FeignCircuitBreaker}, but giving Polaris modification.
 *
 * @author sean yu
 */
public final class PolarisFeignCircuitBreaker {

	private PolarisFeignCircuitBreaker() {
		throw new IllegalStateException("Don't instantiate a utility class");
	}

	/**
	 * @return builder for Feign CircuitBreaker integration
	 */
	public static PolarisFeignCircuitBreaker.Builder builder() {
		return new PolarisFeignCircuitBreaker.Builder();
	}

	/**
	 * Builder for Feign CircuitBreaker integration.
	 */
	public static final class Builder extends Feign.Builder {
		public Builder() {
		}

		public <T> T target(Target<T> target, T fallback) {
			return build(fallback != null ? new FallbackFactory.Default<T>(fallback) : null).newInstance(target);
		}

		public <T> T target(Target<T> target, FallbackFactory<? extends T> fallbackFactory) {
			return build(fallbackFactory).newInstance(target);
		}

		@Override
		public <T> T target(Target<T> target) {
			return build(null).newInstance(target);
		}

		public Feign build(final FallbackFactory<?> nullableFallbackFactory) {
			this.invocationHandlerFactory((target, dispatch) -> new PolarisFeignCircuitBreakerInvocationHandler(
					target, dispatch, nullableFallbackFactory, decoder));
			return this.build();
		}

	}
}
