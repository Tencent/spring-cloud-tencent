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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import com.tencent.cloud.metadata.concurrent.MetadataWrap;

import org.springframework.lang.Nullable;

/**
 * Util methods for Metadata wrapper of jdk executors.
 *
 * @author wlx
 */
public final class MetadataExecutors {

	/**
	 * wrap Executor instance to MetadataExecutorService instance.
	 *
	 * @param executor executor
	 * @return MetadataExecutorService instance
	 */
	public static Executor getMetadataExecutor(Executor executor) {
		if (null == executor || isMetadataWrap(executor)) {
			return executor;
		}
		return new MetadataExecutor(executor);
	}

	/**
	 * wrap ExecutorService instance to MetadataExecutorService instance.
	 *
	 * @param executorService executorService
	 * @return MetadataExecutorService instance
	 */
	public static ExecutorService getMetadataExecutorService(ExecutorService executorService) {
		if (null == executorService || isMetadataWrap(executorService)) {
			return executorService;
		}
		return new MetadataExecutorService(executorService);
	}

	/**
	 * wrap ScheduledExecutorService instance to MetadataScheduledExecutorService instance.
	 *
	 * @param scheduledExecutorService scheduledExecutorService
	 * @return MetadataScheduledExecutorService instance
	 */
	public static ScheduledExecutorService getMetadataScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
		if (null == scheduledExecutorService || isMetadataWrap(scheduledExecutorService)) {
			return scheduledExecutorService;
		}
		return new MetadataScheduledExecutorService(scheduledExecutorService);

	}

	/**
	 * unwrap to the original/underneath one.
	 *
	 * @param executor input executor
	 * @param <T>      Executor type
	 * @return original/underneath one instance.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Executor> T unwrap(@Nullable T executor) {
		if (!isMetadataWrap(executor)) {
			return executor;
		}
		return (T) ((MetadataExecutor) executor).unWrap();
	}

	/**
	 * check the executor is a MetadataExecutor wrapper or not.
	 * <p>
	 * if the parameter executor is MetadataExecutor wrapper, return {@code true}, otherwise {@code false}.
	 * <p>
	 * NOTE: if input executor is {@code null}, return {@code false}.
	 *
	 * @param executor input executor
	 * @param <T>      Executor type
	 * @return         if the parameter executor is MetadataExecutor wrapper
	 */
	public static <T extends Executor> boolean isMetadataWrap(@Nullable T executor) {
		return executor instanceof MetadataWrap;
	}

	private MetadataExecutors() {
	}

}
