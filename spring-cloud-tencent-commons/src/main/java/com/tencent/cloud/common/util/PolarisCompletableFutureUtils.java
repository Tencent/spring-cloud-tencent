/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.tencent.polaris.threadlocal.cross.CompletableFutureUtils;

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
		return CompletableFutureUtils.supplyAsync(supplier, CROSS_THREAD_METADATA_CONTEXT_SUPPLIER, CROSS_THREAD_METADATA_CONTEXT_CONSUMER);
	}

	public static CompletableFuture<Void> runAsync(Runnable runnable) {
		return CompletableFutureUtils.runAsync(runnable, CROSS_THREAD_METADATA_CONTEXT_SUPPLIER, CROSS_THREAD_METADATA_CONTEXT_CONSUMER);
	}
}
