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

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.metadata.concurrent.MetadataCallable;
import com.tencent.cloud.metadata.concurrent.MetadataRunnable;
import org.springframework.lang.NonNull;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * {@link MetadataContext} Wrapper of {@link ScheduledExecutorService},
 * transfer the {@link MetadataContext} from the task submit time of {@link Runnable} or {@link Callable}
 * to the execution time of {@link Runnable} or {@link Callable}.
 *
 * @author wlx
 * @date 2022/7/8 9:40 下午
 */
class MetadataScheduledExecutorService extends MetadataExecutorService
		implements ScheduledExecutorService {

	private final ScheduledExecutorService delegate;

	public MetadataScheduledExecutorService(ScheduledExecutorService delegate) {
		super(delegate);
		this.delegate = delegate;
	}

	@Override
	@NonNull
	public ScheduledFuture<?> schedule(@NonNull Runnable command, long delay, @NonNull TimeUnit unit) {
		return this.delegate.schedule(MetadataRunnable.get(command), delay, unit);
	}

	@Override
	@NonNull
	public <V> ScheduledFuture<V> schedule(@NonNull Callable<V> callable, long delay, @NonNull TimeUnit unit) {
		return this.delegate.schedule(MetadataCallable.get(callable), delay, unit);
	}

	@Override
	@NonNull
	public ScheduledFuture<?> scheduleAtFixedRate(@NonNull Runnable command, long initialDelay,
												  long period, @NonNull TimeUnit unit) {
		return this.delegate.scheduleAtFixedRate(MetadataRunnable.get(command), initialDelay, period, unit);
	}

	@Override
	@NonNull
	public ScheduledFuture<?> scheduleWithFixedDelay(@NonNull Runnable command, long initialDelay,
													 long delay, @NonNull TimeUnit unit) {
		return this.delegate.scheduleAtFixedRate(MetadataRunnable.get(command), initialDelay, delay, unit);
	}

	@Override
	public ScheduledExecutorService unWrap() {
		return this.delegate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MetadataScheduledExecutorService that = (MetadataScheduledExecutorService) o;
		return delegate.equals(that.delegate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(delegate);
	}
}
