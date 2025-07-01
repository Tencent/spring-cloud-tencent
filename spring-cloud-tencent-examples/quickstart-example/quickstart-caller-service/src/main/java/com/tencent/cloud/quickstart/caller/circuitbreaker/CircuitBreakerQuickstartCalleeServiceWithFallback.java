/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.quickstart.caller.circuitbreaker;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * CircuitBreakerQuickstartCalleeServiceWithFallback.
 *
 * @author sean yu
 */
@FeignClient(name = "QuickstartCalleeService", contextId = "fallback-from-code", fallback = CircuitBreakerQuickstartCalleeServiceFallback.class)
public interface CircuitBreakerQuickstartCalleeServiceWithFallback {

	/**
	 * Check circuit break.
	 *
	 * @return circuit break info
	 */
	@GetMapping("/quickstart/callee/circuitBreak")
	String circuitBreak();

	/**
	 * Check circuit break with uid.
	 * @param uid uid variable
	 * @return circuit break info
	 */
	@GetMapping("/circuitBreak/wildcard/{uid}")
	String circuitBreakWildcard(@PathVariable String uid);
}
