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

package com.tencent.cloud.polaris.circuitbreaker.gateway;


import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.ResponseStatusException;

/**
 * PolarisCircuitBreakerFilterFactory.
 *
 * @author seanyu 2023-02-27
 */
public class PolarisCircuitBreakerFilterFactory extends SpringCloudCircuitBreakerFilterFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisCircuitBreakerFilterFactory.class);

	public PolarisCircuitBreakerFilterFactory(ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory,
			ObjectProvider<DispatcherHandler> dispatcherHandlerProvider) {
		super(reactiveCircuitBreakerFactory, dispatcherHandlerProvider);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return super.apply(config);
	}

	@Override
	protected Mono<Void> handleErrorWithoutFallback(Throwable t, boolean resumeWithoutError) {
		if (t instanceof java.util.concurrent.TimeoutException) {
			return Mono.error(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, t.getMessage(), t));
		}
		if (t instanceof CallAbortedException) {
			LOGGER.debug("PolarisCircuitBreaker CallAbortedException: {}", t.getMessage());
			return Mono.error(new ServiceUnavailableException());
		}
		if (resumeWithoutError) {
			return Mono.empty();
		}
		return Mono.error(t);
	}

}
