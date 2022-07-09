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

package com.tencent.cloud.metadata.concurrent;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * {@link MetadataCallable} decorate {@link Callable} to get {@link MetadataContext} value
 * and transfer it to the time of {@link Callable} execution, needed when use {@link Callable} to thread pool.
 * <p>
 * Use factory methods {@link #get} / {@link #gets} to create instance.
 *
 * @author wlx
 * @date 2022/7/8 9:31 下午
 */
public final class MetadataCallable<V> implements Callable<V>,
		MetadataWrap<Callable<V>> {

	private final Callable<V> delegate;

	private final AtomicReference<MetadataContext> metadataContextReference;

	private MetadataCallable(Callable<V> delegate) {
		this.delegate = delegate;
		this.metadataContextReference = new AtomicReference<>(MetadataContextHolder.get());
	}

	@Override
	public V call() throws Exception {
		MetadataContext metadataContext = metadataContextReference.get();
		MetadataContext metadataContextBackup = MetadataContextHolder.get();
		MetadataContextHolder.set(metadataContext);
		try {
			return delegate.call();
		} finally {
			MetadataContextHolder.set(metadataContextBackup);
		}
	}

	/**
	 * Factory method to create  {@link MetadataCallable} instance.
	 *
	 * @param delegate delegate
	 * @param <V>      MetadataCallable return type
	 * @return {@link MetadataCallable} instance
	 */
	public static <V> Callable<V> get(Callable<V> delegate) {
		if (null == delegate || delegate instanceof MetadataCallable) {
			return delegate;
		} else {
			return new MetadataCallable<>(delegate);
		}
	}


	/**
	 * Factory method to create some {@link MetadataCallable} instance.
	 *
	 * @param delegates delegates
	 * @param <V>       MetadataCallable return type
	 * @return some {@link MetadataCallable} instance
	 */
	public static <V> List<Callable<V>> gets(Collection<? extends Callable<V>> delegates) {
		if (delegates == null) {
			return Collections.emptyList();
		}
		return delegates.stream().map(
				MetadataCallable::get
		).collect(Collectors.toList());
	}

	@Override
	public Callable<V> unWrap() {
		return this.delegate;
	}
}
