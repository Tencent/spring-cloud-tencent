package com.tencent.cloud.polaris.circuitbreaker.resttemplate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PolarisCircuitBreakerRestTemplate {

	String fallback() default "";

	Class<? extends PolarisCircuitBreakerFallback> fallbackClass() default PolarisCircuitBreakerFallback.class;

}
