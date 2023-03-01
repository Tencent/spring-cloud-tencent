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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.polaris.circuitbreak.api.InvokeHandler;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.util.context.Context;

/**
 * Reactor Subscriber for PolarisCircuitBreaker.
 *
 * @author seanyu 2023-02-27
 */
public class PolarisCircuitBreakerReactorSubscriber<T> extends BaseSubscriber<T> {

	private final InvokeHandler invokeHandler;


	private final CoreSubscriber<? super T> downstreamSubscriber;

	private final long startTimeMilli;
	private final boolean singleProducer;

	private final AtomicBoolean successSignaled = new AtomicBoolean(false);
	private final AtomicBoolean eventWasEmitted = new AtomicBoolean(false);

	public PolarisCircuitBreakerReactorSubscriber(InvokeHandler invokeHandler, CoreSubscriber<? super T> downstreamSubscriber, boolean singleProducer) {
		this.invokeHandler = invokeHandler;
		this.downstreamSubscriber = downstreamSubscriber;
		this.singleProducer = singleProducer;
		this.startTimeMilli = System.currentTimeMillis();
	}

	@Override
	public Context currentContext() {
		return downstreamSubscriber.currentContext();
	}

	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		downstreamSubscriber.onSubscribe(this);
	}

	@Override
	protected void hookOnNext(T value) {
		if (!isDisposed()) {
			if (singleProducer && successSignaled.compareAndSet(false, true)) {
				long delay = System.currentTimeMillis() - startTimeMilli;
				InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
				responseContext.setDuration(delay);
				responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
				responseContext.setResult(value);
				invokeHandler.onSuccess(responseContext);
			}
			eventWasEmitted.set(true);

			downstreamSubscriber.onNext(value);
		}
	}

	@Override
	protected void hookOnComplete() {
		if (successSignaled.compareAndSet(false, true)) {
			long delay = System.currentTimeMillis() - startTimeMilli;
			InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
			responseContext.setDuration(delay);
			responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
			invokeHandler.onSuccess(responseContext);
		}

		downstreamSubscriber.onComplete();
	}

	@Override
	public void hookOnCancel() {
		if (!successSignaled.get()) {
			if (eventWasEmitted.get()) {
				long delay = System.currentTimeMillis() - startTimeMilli;
				InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
				responseContext.setDuration(delay);
				responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
				invokeHandler.onSuccess(responseContext);
			}
		}
	}

	@Override
	protected void hookOnError(Throwable e) {
		long delay = System.currentTimeMillis() - startTimeMilli;
		InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
		responseContext.setDuration(delay);
		responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
		responseContext.setError(e);
		invokeHandler.onError(responseContext);
		downstreamSubscriber.onError(e);
	}

}
