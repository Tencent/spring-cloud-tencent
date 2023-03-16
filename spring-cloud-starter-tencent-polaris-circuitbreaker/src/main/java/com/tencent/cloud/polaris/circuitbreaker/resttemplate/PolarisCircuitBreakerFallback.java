package com.tencent.cloud.polaris.circuitbreaker.resttemplate;

public interface PolarisCircuitBreakerFallback {

	PolarisCircuitBreakerHttpResponse fallback();

}
