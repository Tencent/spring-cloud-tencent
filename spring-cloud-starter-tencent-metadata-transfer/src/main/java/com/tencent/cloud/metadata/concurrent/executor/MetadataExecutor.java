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

import java.util.Objects;
import java.util.concurrent.Executor;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.metadata.concurrent.MetadataWrap;

import org.springframework.lang.NonNull;

/**
 * {@link MetadataContext} Wrapper of {@link Executor},
 * transfer the {@link MetadataContext} from the task submit time of {@link Runnable}
 * to the execution time of {@link Runnable}.
 *
 * @author wlx
 */
class MetadataExecutor implements Executor, MetadataWrap<Executor> {

	private final Executor delegate;

	MetadataExecutor(Executor delegate) {
		this.delegate = TtlExecutors.getTtlExecutor(delegate);
	}

	@Override
	public void execute(@NonNull Runnable command) {
		delegate.execute(command);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MetadataExecutor that = (MetadataExecutor) o;
		return delegate.equals(that.delegate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(delegate);
	}

	@Override
	public Executor unWrap() {
		return TtlExecutors.unwrap(delegate);
	}

}
