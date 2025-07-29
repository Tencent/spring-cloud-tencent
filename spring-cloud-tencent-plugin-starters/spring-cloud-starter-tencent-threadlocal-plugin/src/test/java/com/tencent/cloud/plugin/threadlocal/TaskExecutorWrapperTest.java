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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 * Test for {@link TaskExecutorWrapper}.
 *
 * @author Haotian Zhang
 */
public class TaskExecutorWrapperTest {

	private static final ThreadLocal<String> TEST_THREAD_LOCAL = new ThreadLocal<>();

	private static final String TEST = "TEST";

	@Test
	public void testExecute() {
		TEST_THREAD_LOCAL.set(TEST);
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.initialize();
		AtomicReference<Boolean> result = new AtomicReference<>(false);
		CountDownLatch latch = new CountDownLatch(1);
		TaskExecutorWrapper<String> taskExecutorWrapper = new TaskExecutorWrapper<>(
				executor, TEST_THREAD_LOCAL::get, TEST_THREAD_LOCAL::set);
		taskExecutorWrapper.execute(() -> {
			result.set(TEST.equals(TEST_THREAD_LOCAL.get()));
			latch.countDown();
		});
		try {
			latch.await();
			assertThat(result.get()).isTrue();
		}
		catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}
}
