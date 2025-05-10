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

package com.tencent.cloud.common.async;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import com.tencent.cloud.plugin.threadlocal.TaskExecutorWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static com.tencent.cloud.common.metadata.CrossThreadMetadataContext.CROSS_THREAD_METADATA_CONTEXT_CONSUMER;
import static com.tencent.cloud.common.metadata.CrossThreadMetadataContext.CROSS_THREAD_METADATA_CONTEXT_SUPPLIER;

/**
 * polaris async executor for @Async .
 *
 * @author Haotian Zhang
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnProperty(name = "spring.cloud.tencent.async.enabled")
public class PolarisAsyncConfiguration implements AsyncConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(PolarisAsyncConfiguration.class);

	@Primary
	@Bean("polarisAsyncExecutor")
	public TaskExecutor polarisAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		int corePoolSize = 10;
		executor.setCorePoolSize(corePoolSize);
		int maxPoolSize = 50;
		executor.setMaxPoolSize(maxPoolSize);
		int queueCapacity = 10;
		executor.setQueueCapacity(queueCapacity);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		String threadNamePrefix = "polaris-async-executor-";
		executor.setThreadNamePrefix(threadNamePrefix);
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(5);
		executor.initialize();
		TaskExecutor executorWrapper = new TaskExecutorWrapper<>(executor, CROSS_THREAD_METADATA_CONTEXT_SUPPLIER, CROSS_THREAD_METADATA_CONTEXT_CONSUMER);
		logger.info("Created async executor with corePoolSize:{}, maxPoolSize:{}, queueCapacity:{}", corePoolSize, maxPoolSize, queueCapacity);
		return executorWrapper;
	}

	@Override
	public Executor getAsyncExecutor() {
		return polarisAsyncExecutor();
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, params) -> logger.error("Execute asynchronous tasks '{}' failed.", method, ex);
	}
}
