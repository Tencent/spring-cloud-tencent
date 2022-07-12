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
 *
 */

package com.tencent.cloud.metadata.concurrent.executor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.alibaba.ttl.threadpool.agent.TtlAgent;
import com.tencent.cloud.common.metadata.MetadataContext;

import org.springframework.lang.NonNull;

/**
 * {@link MetadataContext} Wrapper of {@link ExecutorService},
 * transfer the {@link MetadataContext} from the task submit time of {@link Runnable} or {@link Callable}
 * to the execution time of {@link Runnable} or {@link Callable}.
 *
 * @author wlx
 */
class MetadataExecutorService extends MetadataExecutor implements ExecutorService {

	private final ExecutorService delegate;

	MetadataExecutorService(@NonNull ExecutorService delegate) {
		super(delegate);
		if (TtlAgent.isTtlAgentLoaded() || TtlExecutors.isTtlWrapper(delegate)) {
			this.delegate = delegate;
		}
		else {
			this.delegate = TtlExecutors.getTtlExecutorService(delegate);
		}
	}

	@Override
	public void shutdown() {
		this.delegate.shutdown();
	}

	@Override
	@NonNull
	public List<Runnable> shutdownNow() {
		return this.delegate.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return this.delegate.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return this.delegate.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
		return this.delegate.awaitTermination(timeout, unit);
	}

	@Override
	@NonNull
	public <T> Future<T> submit(@NonNull Callable<T> task) {
		return this.delegate.submit(task);
	}

	@Override
	@NonNull
	public <T> Future<T> submit(@NonNull Runnable task, T result) {
		return this.delegate.submit(task, result);
	}

	@Override
	@NonNull
	public Future<?> submit(@NonNull Runnable task) {
		return this.delegate.submit(task);
	}

	@Override
	@NonNull
	public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return this.delegate.invokeAll(tasks);
	}

	@Override
	@NonNull
	public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		return this.delegate.invokeAll(tasks, timeout, unit);
	}

	@Override
	@NonNull
	public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return this.delegate.invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.delegate.invokeAny(tasks, timeout, unit);
	}

	@Override
	public ExecutorService unwrap() {
		return TtlExecutors.unwrap(delegate);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MetadataExecutorService that = (MetadataExecutorService) o;
		return delegate.equals(that.delegate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(delegate);
	}
}
