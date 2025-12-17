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

package com.tencent.cloud.common.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import com.tencent.polaris.threadlocal.cross.CompletableFutureUtils;
import com.tencent.polaris.threadlocal.cross.SupplierWrapper;
import org.jetbrains.annotations.NotNull;

import org.springframework.util.Assert;
import org.springframework.util.concurrent.FutureUtils;

import static com.tencent.cloud.common.metadata.CrossThreadMetadataContext.CROSS_THREAD_METADATA_CONTEXT_CONSUMER;
import static com.tencent.cloud.common.metadata.CrossThreadMetadataContext.CROSS_THREAD_METADATA_CONTEXT_SUPPLIER;

/**
 * Polaris CompletableFuture Utils.
 *
 * @author Haotian Zhang
 */
public final class PolarisCompletableFutureUtils {

	private PolarisCompletableFutureUtils() {
	}

	public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
		Assert.notNull(supplier, "Supplier must not be null");

		return CompletableFutureUtils.supplyAsync(supplier, CROSS_THREAD_METADATA_CONTEXT_SUPPLIER, CROSS_THREAD_METADATA_CONTEXT_CONSUMER);
	}

	public static CompletableFuture<Void> runAsync(Runnable runnable) {
		Assert.notNull(runnable, "Runnable must not be null");

		return CompletableFutureUtils.runAsync(runnable, CROSS_THREAD_METADATA_CONTEXT_SUPPLIER, CROSS_THREAD_METADATA_CONTEXT_CONSUMER);
	}

	/**
	 * Extended from {@link FutureUtils}.
	 */
	@NotNull
	public static <U> CompletableFuture<U> callAsync(Callable<U> callable) {
		Assert.notNull(callable, "Callable must not be null");

		CompletableFuture<U> result = new CompletableFuture<>();
		Supplier<U> polarisSupplier = new SupplierWrapper<>(toSupplier(callable, result), CROSS_THREAD_METADATA_CONTEXT_SUPPLIER, CROSS_THREAD_METADATA_CONTEXT_CONSUMER);
		return result.completeAsync(polarisSupplier);
	}

	/**
	 * Extended from {@link FutureUtils}.
	 */
	public static <T> CompletableFuture<T> callAsync(Callable<T> callable, Executor executor) {
		Assert.notNull(callable, "Callable must not be null");
		Assert.notNull(executor, "Executor must not be null");

		CompletableFuture<T> result = new CompletableFuture<>();
		Supplier<T> polarisSupplier = new SupplierWrapper<>(toSupplier(callable, result), CROSS_THREAD_METADATA_CONTEXT_SUPPLIER, CROSS_THREAD_METADATA_CONTEXT_CONSUMER);
		return result.completeAsync(polarisSupplier, executor);
	}

	/**
	 * Extended from {@link FutureUtils}.
	 */
	private static <U> Supplier<U> toSupplier(Callable<U> callable, CompletableFuture<U> result) {
		return () -> {
			try {
				return callable.call();
			}
			catch (Exception ex) {
				// wrap the exception just like CompletableFuture::supplyAsync does
				result.completeExceptionally((ex instanceof CompletionException) ? ex : new CompletionException(ex));
				return null;
			}
		};
	}
}
