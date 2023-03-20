package com.tencent.cloud.polaris.circuitbreaker.resttemplate.example;


import java.util.HashMap;

import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisCircuitBreakerFallback;
import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisCircuitBreakerHttpResponse;

import org.springframework.stereotype.Component;

@Component
public class CustomPolarisCircuitBreakerFallback implements PolarisCircuitBreakerFallback {
	@Override
	public PolarisCircuitBreakerHttpResponse fallback() {
		return new PolarisCircuitBreakerHttpResponse(
				200,
				new HashMap<String, String>(){{
					put("Content-Type", "application/json");
				}},
				"{\"msg\": \"this is a fallback class\"}");
	}
}
