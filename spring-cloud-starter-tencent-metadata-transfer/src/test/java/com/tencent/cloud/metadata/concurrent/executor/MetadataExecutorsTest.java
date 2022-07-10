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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * Test for {@link MetadataExecutors}.
 *
 * @author wlx
 */
public class MetadataExecutorsTest {

	@Test
	public void getMetadataExecutor() {
		Executor metadataExecutor = MetadataExecutors.getMetadataExecutor(Executors.newFixedThreadPool(1));
		Assertions.assertThat(metadataExecutor instanceof MetadataExecutor).isTrue();
	}

	@Test
	public void getMetadataExecutorService() {
		ExecutorService metadataExecutorService =
				MetadataExecutors.getMetadataExecutorService(Executors.newFixedThreadPool(1));
		Assertions.assertThat(metadataExecutorService instanceof MetadataExecutorService).isTrue();
	}

	@Test
	public void getMetadataScheduledExecutorService() {
		ScheduledExecutorService metadataScheduledExecutorService =
				MetadataExecutors.getMetadataScheduledExecutorService(Executors.newSingleThreadScheduledExecutor());
		Assertions.assertThat(metadataScheduledExecutorService instanceof MetadataScheduledExecutorService).isTrue();
	}

	@Test
	public void unWrap() {
		ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		ScheduledExecutorService unwrap = MetadataExecutors.unwrap(
				MetadataExecutors.getMetadataScheduledExecutorService(scheduledExecutorService));
		Assertions.assertThat(unwrap).isEqualTo(scheduledExecutorService);
	}

}
