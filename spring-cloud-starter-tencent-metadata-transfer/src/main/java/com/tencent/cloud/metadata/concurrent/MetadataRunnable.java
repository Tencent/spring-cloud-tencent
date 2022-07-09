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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * {@link MetadataRunnable} decorate {@link Runnable} to get {@link MetadataContext} value
 * and transfer it to the time of {@link Runnable} execution, needed when use {@link Runnable} to thread pool.
 * <p>
 * Use factory methods {@link #get} / {@link #gets} to create instance.
 * <p>
 *
 * @author wlx
 * @date 2022/7/8 9:16 下午
 */
public final class MetadataRunnable implements Runnable,
		MetadataWrap<Runnable> {

	private final Runnable delegate;

	private final AtomicReference<MetadataContext> metadataContextReference;

	private MetadataRunnable(Runnable delegate) {
		this.delegate = delegate;
		this.metadataContextReference = new AtomicReference<>(MetadataContextHolder.get());
	}

	@Override
	public void run() {
		MetadataContext metadataContext = metadataContextReference.get();
		MetadataContext metadataContextBackup = MetadataContextHolder.get();
		MetadataContextHolder.set(metadataContext);
		try {
			delegate.run();
		} finally {
			MetadataContextHolder.set(metadataContextBackup);
		}
	}

	/**
	 * Factory method to create {@link MetadataRunnable} instance.
	 *
	 * @param delegate delegate
	 * @return MetadataRunnable instance
	 */
	public static Runnable get(Runnable delegate) {
		if (null == delegate || delegate instanceof MetadataRunnable) {
			return delegate;
		} else {
			return new MetadataRunnable(delegate);
		}
	}

	/**
	 * Factory method to create   {@link MetadataRunnable} instance.
	 *
	 * @param delegates delegates
	 * @return MetadataRunnable instances
	 */
	public static List<Runnable> gets(Collection<Runnable> delegates) {
		if (delegates == null) {
			return Collections.emptyList();
		}
		return delegates.stream().map(
				MetadataRunnable::get
		).collect(Collectors.toList());
	}

	@Override
	public Runnable unWrap() {
		return this.delegate;
	}
}
