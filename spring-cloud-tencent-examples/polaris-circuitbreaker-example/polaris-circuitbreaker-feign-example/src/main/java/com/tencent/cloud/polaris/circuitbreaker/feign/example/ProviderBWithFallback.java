package com.tencent.cloud.polaris.circuitbreaker.feign.example;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "polaris-circuitbreaker-callee-service", contextId = "use-code-fallback", fallback = ProviderBFallback.class)
public interface ProviderBWithFallback {

	/**
	 * Get info of service B.
	 *
	 * @return info of service B
	 */
	@GetMapping("/example/service/b/info")
	String info();

}
