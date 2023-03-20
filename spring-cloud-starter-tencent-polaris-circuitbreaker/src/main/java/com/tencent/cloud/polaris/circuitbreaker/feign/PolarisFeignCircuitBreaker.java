package com.tencent.cloud.polaris.circuitbreaker.feign;

import feign.Feign;
import feign.Target;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.FallbackFactory;

public class PolarisFeignCircuitBreaker {

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

		private CircuitBreakerFactory circuitBreakerFactory;

		private String feignClientName;

		private CircuitBreakerNameResolver circuitBreakerNameResolver;

		public PolarisFeignCircuitBreaker.Builder circuitBreakerFactory(CircuitBreakerFactory circuitBreakerFactory) {
			this.circuitBreakerFactory = circuitBreakerFactory;
			return this;
		}

		public PolarisFeignCircuitBreaker.Builder feignClientName(String feignClientName) {
			this.feignClientName = feignClientName;
			return this;
		}

		public PolarisFeignCircuitBreaker.Builder circuitBreakerNameResolver(CircuitBreakerNameResolver circuitBreakerNameResolver) {
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
			this.invocationHandlerFactory((target, dispatch) -> new PolarisFeignCircuitBreakerInvocationHandler(
					circuitBreakerFactory, feignClientName, target, dispatch, nullableFallbackFactory, circuitBreakerNameResolver, this.decoder));
			return this.build();
		}

	}
}
