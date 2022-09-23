package com.tencent.cloud.polaris.circuitbreaker.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Condition if Polaris circuit breaker is enabled.
 * @author lepdou 2022-09-23
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ConditionalOnPolarisEnabled
@ConditionalOnProperty(name = "spring.cloud.polaris.circuitbreaker.enabled", matchIfMissing = true)
public @interface ConditionalOnPolarisCircuitBreakerEnabled {
}
