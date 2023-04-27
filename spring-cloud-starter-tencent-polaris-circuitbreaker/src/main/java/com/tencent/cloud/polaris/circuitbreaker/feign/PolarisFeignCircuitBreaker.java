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

package com.tencent.cloud.polaris.circuitbreaker.feign;

import java.lang.reflect.Field;

import feign.Feign;
import feign.Target;
import feign.codec.Decoder;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignCircuitBreaker;
import org.springframework.util.ReflectionUtils;

/**
 * PolarisFeignCircuitBreaker, mostly copy from {@link FeignCircuitBreaker}, but giving Polaris modification.
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
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for Feign CircuitBreaker integration.
	 */
	public static final class Builder extends Feign.Builder {

		private CircuitBreakerFactory circuitBreakerFactory;
		private String feignClientName;
		private PolarisCircuitBreakerNameResolver circuitBreakerNameResolver;

		public Builder() {
		}

		public Builder circuitBreakerFactory(CircuitBreakerFactory circuitBreakerFactory) {
			this.circuitBreakerFactory = circuitBreakerFactory;
			return this;
		}

		public Builder feignClientName(String feignClientName) {
			this.feignClientName = feignClientName;
			return this;
		}

		public Builder circuitBreakerNameResolver(PolarisCircuitBreakerNameResolver circuitBreakerNameResolver) {
			this.circuitBreakerNameResolver = circuitBreakerNameResolver;
			return this;
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
			Field field = ReflectionUtils.findField(Feign.Builder.class, "decoder");
			field.setAccessible(true);
			Decoder decoder = (Decoder) ReflectionUtils.getField(field, this);
			this.invocationHandlerFactory((target, dispatch) -> new PolarisFeignCircuitBreakerInvocationHandler(
					circuitBreakerFactory, feignClientName, target, dispatch, nullableFallbackFactory, circuitBreakerNameResolver, decoder));
			return this.build();
		}

	}
}
