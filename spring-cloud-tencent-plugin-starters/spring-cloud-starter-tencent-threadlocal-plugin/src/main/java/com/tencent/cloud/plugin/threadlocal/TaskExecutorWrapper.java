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

package com.tencent.cloud.plugin.threadlocal;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tencent.polaris.threadlocal.cross.RunnableWrapper;

import org.springframework.core.task.TaskExecutor;

public class TaskExecutorWrapper<T> implements TaskExecutor {

	private final TaskExecutor taskExecutor;

	private final Supplier<T> contextGetter;

	private final Consumer<T> contextSetter;

	public TaskExecutorWrapper(TaskExecutor taskExecutor, Supplier<T> contextGetter, Consumer<T> contextSetter) {
		this.taskExecutor = taskExecutor;
		this.contextGetter = contextGetter;
		this.contextSetter = contextSetter;
	}

	@Override
	public void execute(Runnable command) {
		taskExecutor.execute(new RunnableWrapper<>(command, contextGetter, contextSetter));
	}
}
