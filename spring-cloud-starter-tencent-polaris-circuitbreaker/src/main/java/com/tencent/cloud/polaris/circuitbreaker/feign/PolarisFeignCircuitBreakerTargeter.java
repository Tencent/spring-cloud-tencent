package com.tencent.cloud.polaris.circuitbreaker.feign;

import feign.Feign;
import feign.Target;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignCircuitBreaker;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.cloud.openfeign.Targeter;
import org.springframework.util.StringUtils;

public class PolarisFeignCircuitBreakerTargeter implements Targeter {

	private final CircuitBreakerFactory circuitBreakerFactory;

	private final CircuitBreakerNameResolver circuitBreakerNameResolver;

	public PolarisFeignCircuitBreakerTargeter(CircuitBreakerFactory circuitBreakerFactory, CircuitBreakerNameResolver circuitBreakerNameResolver) {
		this.circuitBreakerFactory = circuitBreakerFactory;
		this.circuitBreakerNameResolver = circuitBreakerNameResolver;
	}

	@Override
	public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign, FeignContext context,
			Target.HardCodedTarget<T> target) {
		if (!(feign instanceof PolarisFeignCircuitBreaker.Builder)) {
			return feign.target(target);
		}
		PolarisFeignCircuitBreaker.Builder builder = (PolarisFeignCircuitBreaker.Builder) feign;
		String name = !StringUtils.hasText(factory.getContextId()) ? factory.getName() : factory.getContextId();
		Class<?> fallback = factory.getFallback();
		if (fallback != void.class) {
			return targetWithFallback(name, context, target, builder, fallback);
		}
		Class<?> fallbackFactory = factory.getFallbackFactory();
		if (fallbackFactory != void.class) {
			return targetWithFallbackFactory(name, context, target, builder, fallbackFactory);
		}
		return builder(name, builder).target(target);
	}

	private <T> T targetWithFallbackFactory(String feignClientName, FeignContext context,
			Target.HardCodedTarget<T> target, PolarisFeignCircuitBreaker.Builder builder, Class<?> fallbackFactoryClass) {
		FallbackFactory<? extends T> fallbackFactory = (FallbackFactory<? extends T>) getFromContext("fallbackFactory",
				feignClientName, context, fallbackFactoryClass, FallbackFactory.class);
		return builder(feignClientName, builder).target(target, fallbackFactory);
	}

	private <T> T targetWithFallback(String feignClientName, FeignContext context, Target.HardCodedTarget<T> target,
			PolarisFeignCircuitBreaker.Builder builder, Class<?> fallback) {
		T fallbackInstance = getFromContext("fallback", feignClientName, context, fallback, target.type());
		return builder(feignClientName, builder).target(target, fallbackInstance);
	}

	private <T> T getFromContext(String fallbackMechanism, String feignClientName, FeignContext context,
			Class<?> beanType, Class<T> targetType) {
		Object fallbackInstance = context.getInstance(feignClientName, beanType);
		if (fallbackInstance == null) {
			throw new IllegalStateException(
					String.format("No " + fallbackMechanism + " instance of type %s found for feign client %s",
							beanType, feignClientName));
		}

		if (!targetType.isAssignableFrom(beanType)) {
			throw new IllegalStateException(String.format("Incompatible " + fallbackMechanism
							+ " instance. Fallback/fallbackFactory of type %s is not assignable to %s for feign client %s",
					beanType, targetType, feignClientName));
		}
		return (T) fallbackInstance;
	}

	private PolarisFeignCircuitBreaker.Builder builder(String feignClientName, PolarisFeignCircuitBreaker.Builder builder) {
		return builder
				.circuitBreakerFactory(circuitBreakerFactory)
				.feignClientName(feignClientName)
				.circuitBreakerNameResolver(circuitBreakerNameResolver);
	}

}
