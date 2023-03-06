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

package com.tencent.cloud.polaris.circuitbreaker.reactor;

import com.tencent.polaris.circuitbreak.api.InvokeHandler;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.core.publisher.Operators;

/**
 * MonoOperator for PolarisCircuitBreaker.
 *
 * @author seanyu 2023-02-27
 */
public class PolarisCircuitBreakerMonoOperator<T> extends MonoOperator<T, T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisCircuitBreakerMonoOperator.class);

	private final InvokeHandler invokeHandler;

	protected PolarisCircuitBreakerMonoOperator(Mono<? extends T> source, InvokeHandler invokeHandler) {
		super(source);
		this.invokeHandler = invokeHandler;
	}

	@Override
	public void subscribe(CoreSubscriber<? super T> actual) {
		try {
			invokeHandler.acquirePermission();
			source.subscribe(new PolarisCircuitBreakerReactorSubscriber<>(invokeHandler, actual, true));
		}
		catch (CallAbortedException e) {
			LOGGER.debug("ReactivePolarisCircuitBreaker CallAbortedException: {}", e.getMessage());
			Operators.error(actual, e);
		}
	}

}
