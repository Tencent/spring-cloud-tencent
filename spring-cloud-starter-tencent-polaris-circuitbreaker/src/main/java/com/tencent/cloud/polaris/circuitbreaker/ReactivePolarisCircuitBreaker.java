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

package com.tencent.cloud.polaris.circuitbreaker;

import java.util.function.Function;

import com.tencent.cloud.polaris.circuitbreaker.common.PolarisCircuitBreakerConfigBuilder;
import com.tencent.cloud.polaris.circuitbreaker.common.PolarisResultToErrorCode;
import com.tencent.cloud.polaris.circuitbreaker.reactor.PolarisCircuitBreakerReactorTransformer;
import com.tencent.cloud.polaris.circuitbreaker.util.PolarisCircuitBreakerUtils;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.InvokeHandler;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;

/**
 * ReactivePolarisCircuitBreaker.
 *
 * @author seanyu 2023-02-27
 */
public class ReactivePolarisCircuitBreaker implements ReactiveCircuitBreaker {

	private final InvokeHandler invokeHandler;

	private final ConsumerAPI consumerAPI;

	private final PolarisCircuitBreakerConfigBuilder.PolarisCircuitBreakerConfiguration conf;

	public ReactivePolarisCircuitBreaker(PolarisCircuitBreakerConfigBuilder.PolarisCircuitBreakerConfiguration conf,
			ConsumerAPI consumerAPI,
			CircuitBreakAPI circuitBreakAPI) {
		InvokeContext.RequestContext requestContext = new FunctionalDecoratorRequest(new ServiceKey(conf.getNamespace(), conf.getService()), conf.getMethod());
		requestContext.setSourceService(new ServiceKey(conf.getSourceNamespace(), conf.getSourceService()));
		requestContext.setResultToErrorCode(new PolarisResultToErrorCode());
		this.consumerAPI = consumerAPI;
		this.conf = conf;
		this.invokeHandler = circuitBreakAPI.makeInvokeHandler(requestContext);
	}


	@Override
	public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback) {
		Mono<T> toReturn = toRun.transform(new PolarisCircuitBreakerReactorTransformer<>(invokeHandler));
		if (fallback != null) {
			toReturn = toReturn.onErrorResume(throwable -> {
				if (throwable instanceof CallAbortedException) {
					PolarisCircuitBreakerUtils.reportStatus(consumerAPI, conf, (CallAbortedException) throwable);
				}
				return fallback.apply(throwable);
			});
		}
		return toReturn;
	}

	@Override
	public <T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback) {
		Flux<T> toReturn = toRun.transform(new PolarisCircuitBreakerReactorTransformer<>(invokeHandler));
		if (fallback != null) {
			toReturn = toReturn.onErrorResume(throwable -> {
				if (throwable instanceof CallAbortedException) {
					PolarisCircuitBreakerUtils.reportStatus(consumerAPI, conf, (CallAbortedException) throwable);
				}
				return fallback.apply(throwable);
			});
		}
		return toReturn;
	}

}
